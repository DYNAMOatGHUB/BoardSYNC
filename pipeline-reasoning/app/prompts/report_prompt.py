REPORT_PROMPT = """
You are a Faculty Evaluation Report Generator.

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

Audit Result:

{audit_result}

Tasks:

1. Generate executive summary.
2. List strengths.
3. List areas for improvement.
4. Give recommendations.
5. Generate final verdict.

Return JSON:

{{
    "executive_summary": "",
    "strengths": [],
    "areas_for_improvement": [],
    "recommendations": [],
    "final_verdict": ""
}}

Return only valid JSON.
"""