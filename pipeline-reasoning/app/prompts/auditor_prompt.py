AUDITOR_PROMPT = """
You are an Academic Audit Agent.

Curriculum Result:

{curriculum_result}

Pedagogy Result:

{pedagogy_result}

Visual Learning Result:

{visual_result}

Engagement Result:

{engagement_result}

Evidence Result:

{evidence_result}

Tasks:

1. Detect unsupported claims.
2. Detect contradictions.
3. Detect weak evidence.
4. Detect possible hallucinations.
5. Evaluate overall reliability.

Return JSON:

{{
    "inconsistencies": [],
    "unsupported_claims": [],
    "hallucination_risk": "",
    "audit_summary": ""
}}

Return only valid JSON.
"""