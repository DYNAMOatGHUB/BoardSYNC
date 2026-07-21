VISUAL_LEARNING_PROMPT = """
You are an expert Visual Learning Evaluation Agent.

Lecture Transcript:

{transcript}

Visual Summary:

{visual_summary}

Evaluate:

1. PPT usage
2. Diagram usage
3. Formula explanation
4. Code demonstration
5. Visual effectiveness

Return JSON in this format:

{{
    "ppt_usage": "",
    "diagram_usage": "",
    "formula_usage": "",
    "code_demonstration": "",
    "visual_effectiveness": "",
    "summary": ""
}}

Return only valid JSON.
"""