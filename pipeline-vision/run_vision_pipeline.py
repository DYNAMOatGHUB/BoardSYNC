import cv2
import os
import glob
import time
import argparse
import torch
from skimage.metrics import structural_similarity as ssim
from transformers import Qwen2_5_VLForConditionalGeneration, AutoProcessor
from qwen_vl_utils import process_vision_info

def extract_frames(video_path, frames_dir, motion_threshold=0.98, change_threshold=0.95):
    """
    Step 1: Extracts frames using a 'Stable State' approach with SSIM.
    1. Waits for the screen to STOP changing (consecutive frames very similar = stable).
    2. Only then compares the stable screen to the last SAVED screen.
    This eliminates noisy transition frames (scrolling, swiping, writing in progress).
    """
    print(f"\n--- STEP 1: FRAME EXTRACTION (Stable State) ---")
    
    # Ensure directory exists and clear old frames
    if not os.path.exists(frames_dir):
        os.makedirs(frames_dir)
    else:
        old_frames = glob.glob(os.path.join(frames_dir, "*.jpg"))
        if old_frames:
            print(f"Cleaning up {len(old_frames)} old frames...")
            for f in old_frames:
                os.remove(f)
            
    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        raise ValueError(f"Could not open video {video_path}")
        
    fps = round(cap.get(cv2.CAP_PROP_FPS))
    print(f"Video FPS: {fps}")
    print(f"Motion threshold (stability): {motion_threshold} | Change threshold (uniqueness): {change_threshold}")
    
    frame_count = 0
    saved_count = 0
    last_saved_gray = None
    previous_sec_gray = None
    
    start_time = time.time()
    
    while True:
        ret, frame = cap.read()
        if not ret:
            break
            
        if frame_count % fps == 0:
            # Convert to grayscale and resize for fast SSIM
            small_frame = cv2.resize(frame, (640, 360))
            gray_full = cv2.cvtColor(small_frame, cv2.COLOR_BGR2GRAY)
            
            # Crop out the top 5% and bottom 10% to ignore recording UI elements
            # (status bar, recording timer, playback controls) that cause false duplicates
            h = gray_full.shape[0]
            crop_top = int(h * 0.05)    # Skip top 18px (status bar)
            crop_bottom = int(h * 0.90) # Skip bottom 36px (recording timer/controls)
            gray = gray_full[crop_top:crop_bottom, :]
            
            if last_saved_gray is None:
                # Always save the first frame
                current_sec = frame_count // fps
                mins, secs = divmod(current_sec, 60)
                timestamp_str = f"{mins:02d}m_{secs:02d}s"
                file_name = f"frame_{timestamp_str}.jpg"
                save_path = os.path.join(frames_dir, file_name)
                cv2.imwrite(save_path, frame)
                last_saved_gray = gray
                previous_sec_gray = gray
                saved_count += 1
                elapsed = time.time() - start_time
                print(f"Saved: {file_name} (initial frame) [Elapsed: {elapsed:.1f}s]")
            else:
                # STEP A: Check if the screen is STABLE (not mid-transition)
                motion_score, _ = ssim(previous_sec_gray, gray, full=True)
                
                if motion_score >= motion_threshold:
                    # Screen is stable! Now check if it's DIFFERENT from last saved
                    change_score, _ = ssim(last_saved_gray, gray, full=True)
                    
                    if change_score < change_threshold:
                        # New unique board state detected
                        current_sec = frame_count // fps
                        mins, secs = divmod(current_sec, 60)
                        timestamp_str = f"{mins:02d}m_{secs:02d}s"
                        file_name = f"frame_{timestamp_str}.jpg"
                        save_path = os.path.join(frames_dir, file_name)
                        cv2.imwrite(save_path, frame)
                        last_saved_gray = gray
                        saved_count += 1
                        elapsed = time.time() - start_time
                        print(f"Saved: {file_name} (SSIM vs last saved: {change_score:.3f}) [Elapsed: {elapsed:.1f}s]")
                
                # Always update the previous second's frame for motion detection
                previous_sec_gray = gray
                
        frame_count += 1
        
    cap.release()
    print(f"Extraction complete! {saved_count} unique stable frames saved to {frames_dir}.")


def analyze_frames(frames_dir, output_file):
    """
    Step 2: Analyzes the extracted frames using Qwen2.5-VL and generates the transcript.
    """
    print(f"\n--- STEP 2: VLM ANALYSIS ---")
    frame_paths = sorted(glob.glob(os.path.join(frames_dir, "*.jpg")))
    
    if not frame_paths:
        print("No frames found to analyze!")
        return
        
    print("Loading Qwen2.5-VL-3B-Instruct into VRAM (bfloat16 + SDPA)...")
    model_id = "Qwen/Qwen2.5-VL-3B-Instruct"
    
    # Using 3B as explicitly requested, which natively fits inside the 12GB RTX 5070
    # Using SDPA (Scaled Dot Product Attention) — PyTorch built-in, works on Windows
    model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
        model_id,
        torch_dtype=torch.bfloat16,
        attn_implementation="sdpa",
        device_map="auto"
    )
    processor = AutoProcessor.from_pretrained(model_id)
    
    # Initialize/clear the output file
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("# BoardSync Master Transcript\n\n")
        
    # Strict prompt for high-accuracy OCR + context-aware screen understanding
    system_prompt = (
        "You are an expert pedagogical OCR assistant analyzing a smartboard screen recording. "
        "Each frame is a screenshot from a classroom smartboard. Follow these rules strictly:\n\n"
        "STEP 1 — CLASSIFY THE SCREEN:\n"
        "First, determine what type of screen you are looking at:\n"
        "  A) LECTURE CONTENT — A presentation slide, PPT, document, or whiteboard with educational content.\n"
        "  B) NON-CONTENT SCREEN — A device home screen, lock screen, desktop wallpaper, blank/white screen, "
        "file manager, app launcher, loading screen, or any screen that is NOT educational content.\n\n"
        "STEP 2 — RESPOND BASED ON CLASSIFICATION:\n\n"
        "If (A) LECTURE CONTENT:\n"
        "  1. EXTRACT ALL WRITTEN TEXT exactly as it appears. Preserve headings, bullet points, and emphasis.\n"
        "  2. TRANSCRIBE mathematical equations using LaTeX formatting (e.g., $E = mc^2$).\n"
        "  3. DESCRIBE EVERY DIAGRAM/IMAGE IN DETAIL. Do NOT just name it. For each diagram:\n"
        "     - Describe its visual components (shapes, arrows, cross-sections, labels, colors)\n"
        "     - Explain what the diagram shows\n"
        "     - Note any labels, annotations, or captions visible on or near the diagram\n"
        "     - If there are multiple images/photos, describe each one separately\n"
        "  4. Do NOT describe the presenter, classroom, camera angle, or slide background.\n"
        "  5. Output ONLY the structured Markdown content found on the slide.\n\n"
        "If (B) NON-CONTENT SCREEN:\n"
        "  Output a single short contextual note in brackets describing what the user is doing. Examples:\n"
        "  - '[Device home screen — no lecture content displayed]'\n"
        "  - '[Blank/white screen — possibly loading or transitioning between slides]'\n"
        "  - '[File manager open — user is navigating to open a presentation file]'\n"
        "  - '[App launcher visible — user is switching applications]'\n"
        "  - '[Lock screen with clock display — class has not started yet]'\n"
        "  Do NOT transcribe clock times, app icon names, or other UI text from non-content screens."
    )
    
    for frame_path in frame_paths:
        filename = os.path.basename(frame_path)
        timestamp = filename.replace("frame_", "").replace(".jpg", "")
        print(f"Analyzing {filename}...")
        
        messages = [
            {
                "role": "system",
                "content": [{"type": "text", "text": system_prompt}]
            },
            {
                "role": "user",
                "content": [
                    {
                        "type": "image",
                        "image": frame_path,
                        "max_pixels": 1920 * 1080 # Safeguard against OOM on 4k images
                    },
                    {"type": "text", "text": "Analyze this smartboard frame."},
                ],
            }
        ]
        
        text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        image_inputs, video_inputs = process_vision_info(messages)
        
        inputs = processor(
            text=[text],
            images=image_inputs,
            videos=video_inputs,
            padding=True,
            return_tensors="pt",
        ).to("cuda")
        
        generated_ids = model.generate(**inputs, max_new_tokens=1500)
        
        generated_ids_trimmed = [
            out_ids[len(in_ids):] for in_ids, out_ids in zip(inputs.input_ids, generated_ids)
        ]
        output_text = processor.batch_decode(
            generated_ids_trimmed, skip_special_tokens=True, clean_up_tokenization_spaces=False
        )[0]
        
        with open(output_file, "a", encoding="utf-8") as f:
            f.write(f"## Timestamp: {timestamp}\n\n")
            f.write(output_text.strip() + "\n\n")
            f.write("---\n\n")
            
    print(f"Analysis complete! Context saved to {output_file}.")
    
    # ---------------------------------------------------------
    # CLEANUP (Critical for VRAM management)
    # ---------------------------------------------------------
    print("\n--- CLEANUP ---")
    del model
    del processor
    torch.cuda.empty_cache()
    print("VRAM cleared. Pipeline finished successfully!")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="BoardSync Master Orchestrator")
    parser.add_argument("--video", type=str, default="2026-07-02-08-55-36.mp4", help="Path to input video")
    parser.add_argument("--frames-dir", type=str, default="./test-frames", help="Directory to save extracted frames")
    parser.add_argument("--output", type=str, default="smartboard_context.md", help="Output markdown file")
    
    args = parser.parse_args()
    
    # Run Step 1: Stable-state frame extraction
    extract_frames(args.video, args.frames_dir)
    
    # Run Step 2
    analyze_frames(args.frames_dir, args.output)
