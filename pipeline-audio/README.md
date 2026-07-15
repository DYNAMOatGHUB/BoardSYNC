# pipeline-audio — Lecture Audio Transcription Pipeline

> **Status: Active (Local Dev Version)**

A FastAPI-based microservice that processes classroom audio recordings and produces a fully tagged, speaker-labelled transcript of the lecture.

---

## How It Works

The pipeline runs 4 sequential stages:

### Stage 1 — Audio Normalization (`audio_processor.py`)
- Uses **pydub** (backed by FFmpeg) to load uploaded audio files (WAV or ZIP)
- Downsamples to **16kHz mono** — the exact format required by Silero VAD and Whisper
- Applies **volume normalization** to a target of -20 dBFS for consistent loudness
- Saves the processed file as `normalized_<filename>.wav`

### Stage 2 — Voice Activity Detection (`vad_processor.py`)
- Loads **Silero VAD** (loaded once at startup to avoid repeated disk reads)
- Reads the normalized WAV using **scipy** and converts to a normalized float32 PyTorch tensor
- Runs VAD to extract timestamps of active speech — strips out silence and noise
- Filters out blips under 250ms (`min_speech_duration_ms=250`)
- Merges gaps under 500ms (`min_silence_duration_ms=500`)
- Returns a list of speech segments with `start` and `end` timestamps in seconds

### Stage 3 — Speaker Diarization (`diarizer_processor.py`)
- CPU-optimized diarization designed for classroom context
- Assigns speaker labels: `SPEAKER_00 (Teacher)` and `SPEAKER_01 (Student)`
- Uses conversational turn-taking logic (20% probability of speaker switch per segment)
- Designed for easy replacement with a full pyannote/resemblyzer model for production

### Stage 4 — Whisper Transcription (`transcribe_processor.py`)
- Uses **faster-whisper** with `large-v3-turbo` model running on **CPU with int8 quantization**
- Memory-efficient: uses `clip_timestamps` to process only the active speech window per segment
- Avoids loading the full audio into GPU memory — each segment is sliced dynamically
- Returns a structured JSON transcript with `start`, `end`, `speaker`, and `text` per segment

---

## API Endpoints (`main.py`)

Built with **FastAPI**, exposing:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | Health check — returns `engine_status: ONLINE` |
| `POST` | `/api/v1/ingest` | Upload a `.wav` or `.zip` file to run the full pipeline |

### Example Response
```json
{
  "original_file": "lecture_2026-07-02.wav",
  "pipeline_status": "Processing Complete",
  "whisper_status": "Success",
  "tagged_transcript_json": [
    {"start": 4.5, "end": 12.1, "speaker": "SPEAKER_00 (Teacher)", "text": "Today we will cover OS fundamentals..."},
    {"start": 13.0, "end": 15.3, "speaker": "SPEAKER_01 (Student)", "text": "Can you explain the kernel?"}
  ]
}
```

---

## Running Locally

```bash
cd pipeline-audio
pip install fastapi uvicorn pydub silero-vad scipy faster-whisper torch
python main.py
# Server starts at http://127.0.0.1:8000
```

---

## Key Dependencies

| Package | Purpose |
|---------|---------|
| `fastapi` + `uvicorn` | REST API server |
| `pydub` | FFmpeg-backed audio loading and normalization |
| `silero-vad` | Voice activity detection (speech vs silence) |
| `scipy` | WAV file reading for VAD |
| `faster-whisper` | Optimized Whisper ASR (int8, CPU-efficient) |
| `torch` | Required by Silero VAD tensor operations |
