# BoardSync (Project Staffaract)

A high-performance, AI-powered "digital twin" of classroom and presentation whiteboards.

BoardSync captures the dynamic evolution of a whiteboard during a lecture or presentation, allowing educators and students to instantly replay the "Story" of the board—from blank slate to finished notes—without watching the entire video.

## 💡 The Concept

Most lecture videos start with a blank board and end with a full one. Watching the entire video just to see the notes is highly inefficient.

BoardSync solves this by using **GPU-accelerated diff analysis** to identify exactly when the board changes and automatically extracting only the "Key Frames" (keyframes). It then stitches these frames into a fast-paced, replayable sequence that shows the board's transformation at a glance.

## 🚀 Features

- **🧠 Intelligent Frame Selection**: Uses **Mean Squared Error (MSE)** on the GPU (via CuPy) to detect significant visual changes.
- **⚡ GPU Acceleration**: Leverages NVIDIA CUDA for high-speed frame differencing—essential for analyzing long videos (2+ hours).
- **⏱️ Smart Timestamps**: Automatically generates human-readable timestamps (e.g., `05m_12s`) for each extracted frame.
- **📁 Organized Output**: Saves frames to a structured directory with clear naming conventions.
- **🖼️ High-Quality Extraction**: Preserves the original image quality for clear readability.

## 🛠️ Getting Started

### Prerequisites

- **NVIDIA GPU**: Required for CuPy acceleration.
- **Python 3.8+**
- **CUDA Toolkit**: Installed and configured (usually comes with PyTorch/TensorFlow installations).

### Installation

1.  **Clone the repository** (or copy the script).
2.  **Install dependencies**: 
    ```bash
    pip install opencv-python cupy-cuda12x
    # Or for a specific CUDA version:
    # pip install cupy-cuda118
    ```

### Usage

1.  **Place your video** file (e.g., `your_lecture.mp4`) in the `pipeline-vision/` directory.
2.  **Configure the script**: Open `frame_extractor.py` and modify the constants in the `if __name__ == "__main__"` block:
    ```python
    # ---> SETUP INSTRUCTIONS <---
    # 1. Put your 2-hour .mp4 file in the pipeline-vision folder.
    # 2. Change 'your_2hour_class.mp4' to the exact name of your video file.
    
    VIDEO_FILE = "your_2hour_class.mp4" 
    OUTPUT_DIRECTORY = "test-frames" # or "extracted_keyframes"
    ```
3.  **Run the extractor**:
    ```bash
    python pipeline-vision/frame_extractor.py
    ```

## ⚙️ Configuration & Parameters

The script uses the following key parameters in `extract_unique_frames()`:

- **`video_path`**: Path to the input MP4.
- **`output_folder`**: Where to save the extracted images.
- **`mse_threshold`** (Default: `800.0`):
    - *What it does*: Defines how "different" two frames must be to trigger a save.
    - *Tuning*: Higher values = fewer frames (more aggressive skipping). Lower values = more frames (more frequent saves).
    - *Recommendation*: Start with `800.0`. If you get too many frames, increase it. If you miss important updates, decrease it.

## 🎨 The Output

The script generates a folder (e.g., `test-frames`) containing:

- `frame_00m_00s.jpg` (The initial empty or title slide)
- `frame_01m_34s.jpg` (The first significant board update)
- `frame_02m_15s.jpg`
- ... and so on.

This sequence represents the **visual story** of the lecture, allowing you to see the entire content in seconds.

## 🤝 Contributing

This project is open for expansion. Ideas for future work:
- **Board Detection**: Automatically detecting the physical boundaries of the whiteboard.
- **OCR & Indexing**: Extracting text from frames to create a searchable index.
- **Audio Sync**: Aligning keyframes with the audio transcript.
- **Web Viewer**: A simple UI to replay the extracted frames.

## 📝 License

MIT
