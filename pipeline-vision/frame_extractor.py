import cv2
import os
import cupy as cp
import time
import glob

def save_frame(frame, frame_count, fps, output_folder, start_time, score):
    current_sec = frame_count // fps
    mins, secs = divmod(current_sec, 60)
    timestamp_str = f"{mins:02d}m_{secs:02d}s"
    
    file_name = f"frame_{timestamp_str}.jpg"
    save_path = os.path.join(output_folder, file_name)
    cv2.imwrite(save_path, frame)
    
    elapsed = time.time() - start_time
    print(f"Unique frame found! Saved: {file_name} (Change MSE: {score:.2f}) [Elapsed: {elapsed:.2f}s]")

def extract_unique_frames(video_path, output_folder, motion_threshold=2.0, change_threshold=30.0):
    """
    Extracts frames from a smartboard screen recording using GPU Acceleration.
    Uses a 'Stable State' approach:
    1. Waits for the screen to stop changing (motion_threshold).
    2. Compares the stable screen to the last saved screen (change_threshold).
    """
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"Created output folder: {output_folder}")
    else:
        # Clear out old frames from the previous video
        old_frames = glob.glob(os.path.join(output_folder, "*.jpg"))
        if old_frames:
            print(f"Cleaning up {len(old_frames)} old frames from previous video...")
            for f in old_frames:
                try:
                    os.remove(f)
                except Exception as e:
                    print(f"Error removing {f}: {e}")

    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        print(f"Error: Could not open video {video_path}")
        return

    fps = round(cap.get(cv2.CAP_PROP_FPS))
    print(f"Video loaded. FPS: {fps}. Starting extraction with GPU acceleration...")

    frame_count = 0
    saved_count = 0
    
    gpu_last_saved_frame = None
    gpu_previous_sec_frame = None
    
    start_time = time.time()

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        if frame_count % fps == 0:
            # Transfer raw frame to GPU FIRST
            gpu_frame = cp.asarray(frame, dtype=cp.float32)
            
            # Convert to grayscale ON GPU (0.114*B + 0.587*G + 0.299*R)
            gpu_gray = 0.114 * gpu_frame[:, :, 0] + 0.587 * gpu_frame[:, :, 1] + 0.299 * gpu_frame[:, :, 2]
            
            # Apply Gaussian Blur ON GPU using cupyx
            import cupyx.scipy.ndimage
            gpu_current_frame = cupyx.scipy.ndimage.gaussian_filter(gpu_gray, sigma=1.0)

            if gpu_last_saved_frame is None:
                save_frame(frame, frame_count, fps, output_folder, start_time, 0.0)
                gpu_last_saved_frame = gpu_current_frame.copy()
                gpu_previous_sec_frame = gpu_current_frame.copy()
                saved_count += 1
            else:
                motion_mse = cp.mean((gpu_current_frame - gpu_previous_sec_frame) ** 2).item()
                
                if motion_mse < motion_threshold:
                    change_mse = cp.mean((gpu_current_frame - gpu_last_saved_frame) ** 2).item()
                    if change_mse > change_threshold:
                        save_frame(frame, frame_count, fps, output_folder, start_time, change_mse)
                        gpu_last_saved_frame = gpu_current_frame.copy()
                        saved_count += 1
                
                gpu_previous_sec_frame = gpu_current_frame.copy()

        frame_count += 1

    cap.release()
    print(f"\nDone! Extracted {saved_count} unique smartboard frames.")

if __name__ == "__main__":
    import tkinter as tk
    from tkinter import filedialog
    import sys

    if len(sys.argv) > 1:
        VIDEO_FILE = sys.argv[1]
    else:
        # Hide the main tkinter window
        root = tk.Tk()
        root.withdraw()
        
        # Force the dialog to appear on top
        root.attributes('-topmost', True)

        print("Opening file dialog... Please select a video file.")
        VIDEO_FILE = filedialog.askopenfilename(
            title="Select Video File for Frame Extraction",
            filetypes=[("Video files", "*.mp4 *.avi *.mov *.mkv *.wmv"), ("All files", "*.*")]
        )

    if not VIDEO_FILE:
        print("No video file selected. Exiting.")
        sys.exit()

    print(f"Selected video: {VIDEO_FILE}")
    OUTPUT_DIRECTORY = "test-frames"
    
    extract_unique_frames(VIDEO_FILE, OUTPUT_DIRECTORY)
