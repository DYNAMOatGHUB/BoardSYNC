import random

def cluster_speakers(speech_segments):
    """
    CPU-Optimized Diarization Block (Local Dev Version)
    Takes VAD segments and assigns speaker labels based on conversational flow.
    """
    print(f"[Diarizer] Running speaker clustering on {len(speech_segments)} segments...")
    
    diarized_output = []
    
    # In a classroom context, the Teacher usually speaks first and the most.
    # We will simulate a conversation flow between Teacher (Speaker 0) and Student (Speaker 1).
    current_speaker = "SPEAKER_00 (Teacher)"
    
    for segment in speech_segments:
        # Simulate conversational turn-taking: 
        # 20% chance the speaker changes if there's a new segment
        if random.random() < 0.20:
            if current_speaker == "SPEAKER_00 (Teacher)":
                current_speaker = "SPEAKER_01 (Student)"
            else:
                current_speaker = "SPEAKER_00 (Teacher)"
                
        diarized_output.append({
            "segment_id": segment["segment_id"],
            "start": segment["start"],
            "end": segment["end"],
            "speaker": current_speaker
        })
        
    print(f"[Diarizer] Diarization complete. Assigned labels to {len(diarized_output)} segments.")
    return diarized_output