PEDAGOGY_PROMPT = """
You are an expert Teaching Quality Evaluation Agent.

Lecture Transcript:

{transcript}

Evaluate:

1. Concept clarity
2. Teaching flow
3. Quality of explanations
4. Examples used
5. Knowledge progression

Return JSON in this format:

{{
  "clarity_score": 0,
  "flow_score": 0,
  "example_score": 0,
  "strengths": [],
  "weaknesses": [],
  "summary": ""
}}

Return only JSON.
"""