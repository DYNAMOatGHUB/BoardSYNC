import os
from pydub import AudioSegment

def normalize_audio(input_path: str, target_dbfs: float = -20.0) -> str:
    """
    FFmpeg Normalization Block:
    1. Loads the uploaded audio file.
    2. Converts/Downsamples to 16kHz, Mono.
    3. Normalizes the volume level.
    4. Saves the output to a new file.
    """
    print(f"[FFmpeg] Starting normalization for: {input_path}")
    
    # Load the audio file (Pydub handles WAV, MP3, etc. via FFmpeg underlying)
    audio = AudioSegment.from_file(input_path)
    
    # 1. Downsample to 16kHz and convert to Mono (1 channel)
    # This exactly matches the strict (16kHz mono) requirements in your diagram
    audio = audio.set_frame_rate(16000).set_channels(1)
    
    # 2. Volume Normalization
    # Calculate how much to change the volume to hit the target decibel level
    change_in_db = target_dbfs - audio.dBFS
    normalized_audio = audio.apply_gain(change_in_db)
    
    # 3. Save the normalized audio to a new file
    base_dir = os.path.dirname(input_path)
    base_name = os.path.basename(input_path)
    output_filename = f"normalized_{os.path.splitext(base_name)[0]}.wav"
    output_path = os.path.join(base_dir, output_filename)
    
    normalized_audio.export(output_path, format="wav")
    print(f"[FFmpeg] Normalized audio successfully saved to: {output_path}")
    
    return output_path