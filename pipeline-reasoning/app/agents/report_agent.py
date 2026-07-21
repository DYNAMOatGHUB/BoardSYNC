import logging
from app.agents.base_agent import BaseAgent
from app.models.schemas import (
    CurriculumResult,
    PedagogyResult,
    VisualLearningResult,
    EngagementResult,
    EvidenceResult,
    AuditResult,
    FacultyEvaluationReport
)
from app.prompts.report_prompt import REPORT_PROMPT

logger = logging.getLogger(__name__)


class ReportAgent(BaseAgent):
    """
    Faculty Evaluation Report Agent.
    Compiles all validated findings and audit results into the final comprehensive report.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        super().__init__(model_name=model_name, temperature=temperature)

    def run(
        self,
        curriculum_result: CurriculumResult,
        pedagogy_result: PedagogyResult,
        visual_result: VisualLearningResult,
        engagement_result: EngagementResult,
        evidence_result: EvidenceResult,
        audit_result: AuditResult
    ) -> FacultyEvaluationReport:
        """
        Executes the final report generation.
        """
        self.logger.info("Running ReportAgent")
        
        prompt = self.format_prompt(
            REPORT_PROMPT,
            curriculum_result=curriculum_result,
            pedagogy_result=pedagogy_result,
            visual_result=visual_result,
            engagement_result=engagement_result,
            evidence_result=evidence_result,
            audit_result=audit_result
        )
        
        try:
            return self.get_structured_output(prompt, FacultyEvaluationReport)
        except Exception as e:
            self.logger.error(f"ReportAgent failed: {e}", exc_info=True)
            # Return a fallback FacultyEvaluationReport
            return FacultyEvaluationReport(
                executive_summary=f"Error during report compilation: {str(e)}",
                strengths=[],
                areas_for_improvement=[],
                recommendations=[],
                final_verdict="Verification incomplete due to system errors"
            )