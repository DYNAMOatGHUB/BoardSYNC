from pydantic import BaseModel, Field
from typing import List, Optional


# =====================================================
# SHARED INPUT
# =====================================================

class TeachingContext(BaseModel):

    faculty_name: str

    subject: str

    session_id: str

    transcript: str

    visual_summary: str

    rag_context: str


# =====================================================
# CURRICULUM AGENT
# =====================================================

class CurriculumResult(BaseModel):

    coverage_percentage: int

    covered_topics: List[str]

    missing_topics: List[str]

    extra_topics: List[str]

    summary: str


# =====================================================
# PEDAGOGY AGENT
# =====================================================

class PedagogyResult(BaseModel):

    clarity_score: int

    flow_score: int

    example_score: int

    strengths: List[str]

    weaknesses: List[str]

    summary: str


# =====================================================
# VISUAL LEARNING AGENT
# =====================================================

class VisualLearningResult(BaseModel):

    ppt_usage: str

    diagram_usage: str

    formula_usage: str

    code_demonstration: str

    visual_effectiveness: str

    summary: str


# =====================================================
# ENGAGEMENT AGENT
# =====================================================

class EngagementResult(BaseModel):

    interaction_level: str

    question_frequency: str

    participation_level: str

    discussion_quality: str

    summary: str


# =====================================================
# EVIDENCE AGENT
# =====================================================

class EvidenceResult(BaseModel):

    evidence_points: List[str]

    supporting_observations: List[str]

    summary: str


# =====================================================
# AUDITOR AGENT
# =====================================================

class AuditResult(BaseModel):

    inconsistencies: List[str]

    unsupported_claims: List[str]

    hallucination_risk: str

    audit_summary: str


# =====================================================
# FINAL REPORT
# =====================================================

class FacultyEvaluationReport(BaseModel):

    executive_summary: str

    strengths: List[str]

    areas_for_improvement: List[str]

    recommendations: List[str]

    final_verdict: str


# =====================================================
# LANGGRAPH STATE
# =====================================================

class AgentState(BaseModel):

    context: TeachingContext

    curriculum_result: Optional[CurriculumResult] = None

    pedagogy_result: Optional[PedagogyResult] = None

    visual_result: Optional[VisualLearningResult] = None

    engagement_result: Optional[EngagementResult] = None

    evidence_result: Optional[EvidenceResult] = None

    audit_result: Optional[AuditResult] = None

    final_report: Optional[FacultyEvaluationReport] = None