import os
import shutil
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from audio_processor import normalize_audio
from vad_processor import detect_voice_activity
from diarizer_processor import cluster_speakers
# Import the final transcription block
from transcribe_processor import transcribe_audio_segments

app = FastAPI(title="Edge Smartboard Ingestion Engine")

@app.get("/")
async def root():
    return {"engine_status": "ONLINE"}

@app.post("/api/v1/ingest")
async def ingest_session(file: UploadFile = File(...)):
    file_extension = os.path.splitext(file.filename)[1].lower()
    if file_extension not in [".zip", ".wav"]:
        raise HTTPException(status_code=400, detail="Invalid file type.")

    target_path = os.path.join("./received_sessions", file.filename)
    try:
        with open(target_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

    analysis_result = execute_audio_pipeline(target_path)

    return JSONResponse(status_code=200, content={"status": "success", "pipeline_output": analysis_result})

def execute_audio_pipeline(file_path: str):
    # 1. Run FFmpeg Normalization / Unzip
    actual_audio_path = file_path
    if file_path.lower().endswith(".zip"):
        try:
            import zipfile
            extract_dir = os.path.splitext(file_path)[0]
            os.makedirs(extract_dir, exist_ok=True)
            with zipfile.ZipFile(file_path, "r") as zip_ref:
                zip_ref.extractall(extract_dir)
            
            # Walk and find the first .wav file
            wav_files = []
            for root, _, files in os.walk(extract_dir):
                for file in files:
                    if file.lower().endswith(".wav"):
                        wav_files.append(os.path.join(root, file))
            
            if not wav_files:
                return {"error": "No .wav file found in the extracted zip archive."}
            actual_audio_path = wav_files[0]
        except Exception as e:
            return {"error": f"Zip extraction failed: {str(e)}"}

    try:
        normalized_file_path = normalize_audio(actual_audio_path)
    except Exception as e:
        return {"error": f"FFmpeg failed: {str(e)}"}

    # 2. Run Silero VAD
    try:
        speech_segments = detect_voice_activity(normalized_file_path)
        if not speech_segments:
            return {"status": "No speech detected"}
    except Exception as e:
        return {"error": f"VAD failed: {str(e)}"}

    # 3. Run CPU Speaker Diarization
    try:
        diarized_segments = cluster_speakers(speech_segments)
    except Exception as e:
        return {"error": f"Diarization failed: {str(e)}"}

    # 4. Run Whisper-Turbo Transcription
    try:
        # Pass all diarized segments for transcription
        full_transcript = transcribe_audio_segments(normalized_file_path, diarized_segments)
        whisper_status = "Success"
    except Exception as e:
        full_transcript = []
        whisper_status = f"Failed: {str(e)}"

    return {
        "original_file": os.path.basename(file_path),
        "pipeline_status": "Processing Complete",
        "whisper_status": whisper_status,
        "tagged_transcript_json": full_transcript
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)