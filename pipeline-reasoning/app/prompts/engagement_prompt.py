ENGAGEMENT_PROMPT = """
You are an expert Classroom Engagement Evaluation Agent.

Lecture Transcript:

{transcript}

Evaluate:

1. Interaction level
2. Question frequency
3. Student participation
4. Discussion quality
5. Engagement effectiveness

Return JSON in this format:

{{
    "interaction_level": "",
    "question_frequency": "",
    "participation_level": "",
    "discussion_quality": "",
    "summary": ""
}}

Return only valid JSON.
"""