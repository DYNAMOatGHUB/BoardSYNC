from faster_whisper import WhisperModel

print("[Whisper] Initializing Whisper-Turbo on CPU (int8 optimization)...")
# Using 'large-v3-turbo' as specified in your architecture. 
# Running on 'cpu' with 'int8' keeps it highly RAM-efficient for your laptop.
model = WhisperModel("large-v3-turbo", device="cpu", compute_type="int8")

def transcribe_audio_segments(audio_path: str, diarized_segments: list) -> list:
    """
    GPU Worker Pool Block (Local CPU-Optimized Variant): Whisper-Turbo ASR
    Iterates through your diarized timeline and generates text transcripts.
    """
    print(f"[Whisper] Starting text transcription for: {audio_path}")
    final_transcript = []

    for seg in diarized_segments:
        print(f"[Whisper] Transcribing segment {seg['segment_id']} ({seg['start']}s -> {seg['end']}s)")
        
        # FIX: We use clip_timestamps to slice the audio memory dynamically
        segments, info = model.transcribe(
            audio_path,
            beam_size=1,
            clip_timestamps=f"{seg['start']},{seg['end']}" # Forces Whisper to only look at this window
        )
        
        text_content = " ".join([segment.text for segment in segments]).strip()
        
        final_transcript.append({
            "start": seg["start"],
            "end": seg["end"],
            "speaker": seg["speaker"],
            "text": text_content
        })
        
    print(f"[Whisper] Transcription complete for {len(final_transcript)} segments.")
    return final_transcript