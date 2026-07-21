from pydantic import BaseModel
from typing import Optional

from app.models.schemas import (
    TeachingContext,
    CurriculumResult,
    PedagogyResult,
    VisualLearningResult,
    EngagementResult,
    EvidenceResult,
    AuditResult,
    FacultyEvaluationReport
)


class AgentState(BaseModel):

    context: TeachingContext

    curriculum_result: Optional[CurriculumResult] = None

    pedagogy_result: Optional[PedagogyResult] = None

    visual_result: Optional[VisualLearningResult] = None

    engagement_result: Optional[EngagementResult] = None

    evidence_result: Optional[EvidenceResult] = None

    audit_result: Optional[AuditResult] = None

    final_report: Optional[FacultyEvaluationReport] = None