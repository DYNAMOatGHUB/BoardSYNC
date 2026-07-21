import logging
from app.agents.base_agent import BaseAgent
from app.models.schemas import (
    TeachingContext,
    CurriculumResult,
    PedagogyResult,
    VisualLearningResult,
    EngagementResult,
    EvidenceResult
)
from app.prompts.evidence_prompt import EVIDENCE_PROMPT

logger = logging.getLogger(__name__)


class EvidenceAgent(BaseAgent):
    """
    Evidence Validation Agent.
    Validates findings against transcript, extracts supporting snippets, and filters claims.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        super().__init__(model_name=model_name, temperature=temperature)

    def run(
        self,
        context: TeachingContext,
        curriculum_result: CurriculumResult,
        pedagogy_result: PedagogyResult,
        visual_result: VisualLearningResult,
        engagement_result: EngagementResult
    ) -> EvidenceResult:
        """
        Executes the evidence validation analysis.
        """
        self.logger.info(f"Running EvidenceAgent for faculty: {context.faculty_name}")
        
        prompt = self.format_prompt(
            EVIDENCE_PROMPT,
            transcript=context.transcript,
            curriculum_result=curriculum_result,
            pedagogy_result=pedagogy_result,
            visual_result=visual_result,
            engagement_result=engagement_result
        )
        
        try:
            return self.get_structured_output(prompt, EvidenceResult)
        except Exception as e:
            self.logger.error(f"EvidenceAgent failed: {e}", exc_info=True)
            # Return a fallback EvidenceResult
            return EvidenceResult(
                evidence_points=[],
                supporting_observations=[],
                summary=f"Error during evidence validation analysis: {str(e)}"
            )