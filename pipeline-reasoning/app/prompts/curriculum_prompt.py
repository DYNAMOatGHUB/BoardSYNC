CURRICULUM_PROMPT = """
You are an expert Curriculum Evaluation Agent.

Expected Curriculum Topics:

{rag_context}

Lecture Transcript:

{transcript}

Tasks:

1. Identify covered topics.
2. Identify missing topics.
3. Identify additional topics.
4. Estimate curriculum coverage percentage.
5. Write a concise summary.

Return JSON in this format:

{{
    "coverage_percentage": 0,
    "covered_topics": [],
    "missing_topics": [],
    "extra_topics": [],
    "summary": ""
}}

Return only valid JSON.
"""