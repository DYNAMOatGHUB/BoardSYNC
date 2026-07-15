import os
import glob
import torch
from transformers import Qwen2_5_VLForConditionalGeneration, AutoProcessor, BitsAndBytesConfig
from qwen_vl_utils import process_vision_info

def analyze_frames(frames_dir="./test-frames", output_file="smartboard_context.md"):
    """
    Analyzes smartboard frames using Qwen2.5-VL-7B-Instruct and generates a Markdown transcript.
    Designed for memory-constrained environments (e.g. 8GB-12GB VRAM).
    """
    print("Loading model Qwen/Qwen2.5-VL-7B-Instruct (Optimized for 8GB/12GB VRAM)...")
    
    # ---------------------------------------------------------
    # 1. Model Loading (Optimized for RTX 4060 8GB / RTX 5070 12GB)
    # ---------------------------------------------------------
    # Qwen2.5-VL comes in 3B, 7B, and 72B. The 7B model requires ~14GB VRAM in standard 16-bit precision.
    # To run on the local 8GB RTX 4060 for testing, we use 4-bit quantization.
    # NOTE: When deploying to the 12GB RTX 5070 server, change `load_in_4bit` to `load_in_8bit` for max precision!
    model_id = "Qwen/Qwen2.5-VL-7B-Instruct"
    
    quantization_config = BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_compute_dtype=torch.bfloat16
    )
    
    model = Qwen2_5_VLForConditionalGeneration.from_pretrained(
        model_id,
        quantization_config=quantization_config,
        attn_implementation="eager",  # flash_attention_2 requires extra install; use eager for local dev
        device_map="auto"
    )
    
    processor = AutoProcessor.from_pretrained(model_id)
    
    # ---------------------------------------------------------
    # 2. Data Ingestion
    # ---------------------------------------------------------
    # Read all .jpg files from the frames directory and sort alphabetically
    # This ensures chronologial order (e.g. frame_00m_06s.jpg -> frame_01m_58s.jpg)
    frame_paths = sorted(glob.glob(os.path.join(frames_dir, "*.jpg")))
    
    if not frame_paths:
        print(f"No frames found in {frames_dir}. Please run frame_extractor.py first.")
        return
        
    print(f"Found {len(frame_paths)} frames. Starting pedagogical OCR analysis...")
    
    # Ensure output file is fresh before appending
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("# Smartboard Transcription\n\n")
    
    # ---------------------------------------------------------
    # 3. VLM Prompting & Processing Loop
    # ---------------------------------------------------------
    system_prompt = (
        "You are a pedagogical OCR assistant. Your task is to extract all written text, "
        "format mathematical equations in LaTeX, and describe the structure of any diagrams "
        "present on the board. Explicitly ignore the presenter, classroom environment, and camera angle. "
        "Output ONLY the extracted content and diagram descriptions."
    )
    
    for frame_path in frame_paths:
        # Extract timestamp from filename (e.g., 'frame_14m_32s.jpg' -> '14m_32s')
        filename = os.path.basename(frame_path)
        timestamp = filename.replace("frame_", "").replace(".jpg", "")
        
        print(f"\nProcessing {filename}...")
        
        # Construct the chat template for Qwen-VL
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
                        # Cap the resolution to 1080p max to prevent OOM on 12GB VRAM
                        # Qwen2.5-VL dynamically parses resolution; capping it guarantees it fits.
                        "max_pixels": 1920 * 1080 
                    },
                    {"type": "text", "text": "Analyze this smartboard frame."},
                ],
            }
        ]
        
        # Preprocess the inputs using Qwen's utilities
        text = processor.apply_chat_template(
            messages, tokenize=False, add_generation_prompt=True
        )
        image_inputs, video_inputs = process_vision_info(messages)
        
        inputs = processor(
            text=[text],
            images=image_inputs,
            videos=video_inputs,
            padding=True,
            return_tensors="pt",
        )
        
        # Move inputs to the appropriate device (CUDA)
        inputs = inputs.to("cuda")

        # Generate output (inference)
        # Using a reasonable max_new_tokens for a board transcript
        generated_ids = model.generate(**inputs, max_new_tokens=1500)
        
        # ---------------------------------------------------------
        # 4. Output Generation
        # ---------------------------------------------------------
        # Post-process to extract only the newly generated text (ignoring the prompt)
        generated_ids_trimmed = [
            out_ids[len(in_ids):] for in_ids, out_ids in zip(inputs.input_ids, generated_ids)
        ]
        
        output_text = processor.batch_decode(
            generated_ids_trimmed, skip_special_tokens=True, clean_up_tokenization_spaces=False
        )[0]
        
        # Append the results to the markdown file cleanly
        with open(output_file, "a", encoding="utf-8") as f:
            f.write(f"## Timestamp: {timestamp}\n\n")
            f.write(output_text.strip() + "\n\n")
            f.write("---\n\n")
            
        print(f"Appended analysis for {filename} to {output_file}.")

    print(f"\nAll frames successfully analyzed. Results saved to {output_file}.")
    
    # ---------------------------------------------------------
    # 5. Cleanup
    # ---------------------------------------------------------
    print("Cleaning up VRAM for the next pipeline step...")
    del model
    del processor
    torch.cuda.empty_cache()
    print("VRAM cleared.")

if __name__ == "__main__":
    # You can customize these paths if you are running it from a different working directory
    FRAMES_DIR = "test-frames"
    OUTPUT_FILE = "smartboard_context.md"
    
    analyze_frames(FRAMES_DIR, OUTPUT_FILE)
