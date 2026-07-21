import logging
from app.agents.base_agent import BaseAgent
from app.models.schemas import (
    CurriculumResult,
    PedagogyResult,
    VisualLearningResult,
    EngagementResult,
    EvidenceResult,
    AuditResult
)
from app.prompts.auditor_prompt import AUDITOR_PROMPT

logger = logging.getLogger(__name__)


class AuditorAgent(BaseAgent):
    """
    Academic Audit Agent.
    Audits previous evaluation steps to flag inconsistencies, hallucination risks, and unsubstantiated claims.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        super().__init__(model_name=model_name, temperature=temperature)

    def run(
        self,
        curriculum_result: CurriculumResult,
        pedagogy_result: PedagogyResult,
        visual_result: VisualLearningResult,
        engagement_result: EngagementResult,
        evidence_result: EvidenceResult
    ) -> AuditResult:
        """
        Executes the academic audit analysis.
        """
        self.logger.info("Running AuditorAgent")
        
        prompt = self.format_prompt(
            AUDITOR_PROMPT,
            curriculum_result=curriculum_result,
            pedagogy_result=pedagogy_result,
            visual_result=visual_result,
            engagement_result=engagement_result,
            evidence_result=evidence_result
        )
        
        try:
            return self.get_structured_output(prompt, AuditResult)
        except Exception as e:
            self.logger.error(f"AuditorAgent failed: {e}", exc_info=True)
            # Return a fallback AuditResult
            return AuditResult(
                inconsistencies=[],
                unsupported_claims=[],
                hallucination_risk="Unknown",
                audit_summary=f"Error during academic audit: {str(e)}"
            )