import torch
from scipy.io import wavfile
import numpy as np
from silero_vad import load_silero_vad, get_speech_timestamps

# Initialize the model once so it stays in memory
# This prevents reloading the model from disk on every single request
print("[VAD] Loading Silero VAD model...")
model = load_silero_vad()

def load_audio_scipy(path: str) -> torch.Tensor:
    """
    Loads a WAV file using scipy, downmixing to mono and normalizing 
    samples to float32 values between -1.0 and 1.0.
    """
    sr, y = wavfile.read(path)
    
    # If stereo/multichannel, convert to mono by taking the mean across channels
    if len(y.shape) > 1:
        y = y.mean(axis=1)
        
    # Convert to float32 normalized in [-1.0, 1.0]
    if y.dtype == np.int16:
        y = y.astype(np.float32) / 32768.0
    elif y.dtype == np.int32:
        y = y.astype(np.float32) / 2147483648.0
    elif y.dtype == np.uint8:
        y = (y.astype(np.float32) - 128.0) / 128.0
    elif y.dtype == np.float32:
        pass
    else:
        y = y.astype(np.float32)
        
    return torch.from_numpy(y)

def detect_voice_activity(audio_path: str):
    """
    Silero VAD Block:
    Reads the normalized 16kHz WAV file and extracts timestamps 
    where speech is actively happening, stripping out silence.
    """
    print(f"[VAD] Scanning audio for speech segments: {audio_path}")
    
    # Read the audio into a format PyTorch understands
    wav = load_audio_scipy(audio_path)
    
    # Get speech timestamps (sampling_rate must match our 16000Hz normalization)
    speech_timestamps = get_speech_timestamps(
        wav, 
        model, 
        sampling_rate=16000,
        min_speech_duration_ms=250,  # Ignore blips shorter than 250ms
        min_silence_duration_ms=500   # Merge speech gaps shorter than 500ms
    )
    
    # Format the results cleanly for our logs/response
    formatted_segments = []
    for i, ts in enumerate(speech_timestamps):
        # Silero returns timestamps in raw audio samples; convert them to seconds
        start_sec = round(ts['start'] / 16000, 2)
        end_sec = round(ts['end'] / 16000, 2)
        formatted_segments.append({"segment_id": i, "start": start_sec, "end": end_sec})
        
    print(f"[VAD] Scan complete. Found {len(formatted_segments)} active speech segments.")
    return formatted_segments
