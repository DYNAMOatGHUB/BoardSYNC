# BoardSYNC

> **AI-powered lecture evaluation and faculty performance reporting platform**

BoardSYNC runs on a computation server and automatically analyzes lecture recordings to evaluate teaching quality — generating detailed, objective reports on how well a faculty member delivered their class.

---

## What It Does

BoardSYNC takes a lecture video as input and produces a comprehensive evaluation report covering:
- **Content Coverage** — What was written/taught on the board vs. what was expected
- **Structured Transcription** — Every board state captured and transcribed into Markdown
- **Teaching Flow Analysis** — How content evolved over time (via timestamps)
- **Completeness Assessment** — Was the curriculum delivered fully?

The goal is to give institutions, HoDs, and faculty themselves an objective, data-driven view of each class — without manually watching hours of video.

---

## Architecture

BoardSYNC is a modular multi-pipeline system running on a computation server. Each pipeline is independently deployable and feeds into the next:

```
BoardSYNC/
│
├── pipeline-vision/        ✅ ACTIVE  — Smartboard frame extraction + VLM transcription
├── pipeline-audio/         ✅ ACTIVE  — Audio normalization, VAD, diarization, Whisper ASR
├── pipeline-reasoning/     🔜 PLANNED — Evaluation engine, scoring, report generation
├── backend-core/           🔜 PLANNED — API server, database, auth
├── frontend-dashboard/     🔜 PLANNED — Student/educator web UI
└── edge-apk/               🔜 PLANNED — Android APK for on-device capture
```

---

## pipeline-vision — Smartboard Transcription Pipeline

**Status: ✅ Active**

The first completed pipeline. Processes lecture video to extract and transcribe every unique board state.

### How It Works

**Step 1 — Stable-State Frame Extraction** (`frame_extractor.py` + `run_vision_pipeline.py`)
- Uses **SSIM (Structural Similarity Index)** to detect when the board is in a *stable state* — i.e., the faculty member has stopped writing and the content is settled
- Skips noisy transition frames (mid-write, hand movements, scrolling)
- Crops out recording overlays (status bar, timer) to prevent false-change detection
- Only saves frames that are meaningfully different from the previous saved state
- Configurable thresholds: `motion_threshold` (default: `0.98`) and `change_threshold` (default: `0.95`)

**Step 2 — VLM Board Reading** (`vision_agent.py`)
- Loads **Qwen2.5-VL-7B-Instruct** with **4-bit quantization** (runs on 8GB–12GB VRAM)
- Pedagogical OCR system prompt: extracts written text, formats equations as **LaTeX**, describes diagrams
- Ignores the presenter, students, and classroom environment — only reads the board
- Outputs a structured Markdown file (`smartboard_context.md`) with one section per timestamp

### Quick Start

```bash
# From the pipeline-vision directory
cd pipeline-vision

# Activate venv
.\venv\Scripts\activate        # Windows
# source venv/bin/activate     # Linux/macOS

# Run the full pipeline on a lecture recording
python run_vision_pipeline.py --video "lecture_recording.mp4"

# Arguments:
#   --video        Path to the lecture video (required)
#   --frames-dir   Where to save extracted frames (default: ./test-frames)
#   --output       Output report file (default: smartboard_context.md)
```

### Output Example

```markdown
# Smartboard Transcription

## Timestamp: 06m_03s
Slide: "OS FOUNDATIONS & KERNEL ARCHITECTURE"
Topics covered: Linux Systems, OS Overview, Control Flow, UI, App Management

## Timestamp: 06m_54s
### Control Flow Diagram
User -> Application -> Operating System -> Hardware
```

### Hardware Requirements (Computation Server)

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| GPU VRAM  | 8 GB (4-bit) | 12 GB (8-bit) |
| GPU       | RTX 3060 | RTX 4070 / RTX 5070 |
| RAM       | 16 GB | 32 GB |
| Python    | 3.10+ | 3.11+ |

---

## Upcoming Pipelines

| Module | Purpose | Status |
|--------|---------|--------|
| `pipeline-audio` | FFmpeg normalization → Silero VAD → speaker diarization → Whisper-Turbo ASR | ✅ Active |
| `pipeline-reasoning` | Evaluation engine — scores teaching quality, generates faculty report | Planned |
| `backend-core` | FastAPI server, PostgreSQL, job queue (Celery), auth | Planned |
| `frontend-dashboard` | Web dashboard for HoDs, admin, and faculty self-review | Planned |
| `edge-apk` | Android app for automated classroom recording | Planned |

---

## Evaluation Report (End Goal)

Once all pipelines are connected, BoardSYNC will generate a structured faculty evaluation report like:

```
Faculty Evaluation Report — Dr. [Name]
Course: Operating Systems | Date: 2026-07-02

Board Coverage Score:     87/100
Topics Delivered:         5 / 6 planned
Avg Board Legibility:     High
Teaching Pace:            Moderate (30 stable states in 45 min)
Audio Clarity:            Delivered by pipeline-audio (Whisper-Turbo ASR)
Content Accuracy:         [pipeline-reasoning — coming soon]
```

---

## License

MIT
