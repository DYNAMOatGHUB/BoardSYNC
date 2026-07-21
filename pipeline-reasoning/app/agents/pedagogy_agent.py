import logging
from app.agents.base_agent import BaseAgent
from app.models.schemas import TeachingContext, PedagogyResult
from app.prompts.pedagogy_prompt import PEDAGOGY_PROMPT

logger = logging.getLogger(__name__)


class PedagogyAgent(BaseAgent):
    """
    Pedagogy Evaluation Agent.
    Evaluates classroom teaching quality, concepts clarity, and flow.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        super().__init__(model_name=model_name, temperature=temperature)

    def run(self, context: TeachingContext) -> PedagogyResult:
        """
        Executes the pedagogy evaluation analysis.
        """
        self.logger.info(f"Running PedagogyAgent for faculty: {context.faculty_name}")
        
        prompt = self.format_prompt(
            PEDAGOGY_PROMPT,
            transcript=context.transcript
        )
        
        try:
            return self.get_structured_output(prompt, PedagogyResult)
        except Exception as e:
            self.logger.error(f"PedagogyAgent failed: {e}", exc_info=True)
            # Return a fallback PedagogyResult
            return PedagogyResult(
                clarity_score=0,
                flow_score=0,
                example_score=0,
                strengths=[],
                weaknesses=[],
                summary=f"Error during pedagogy analysis: {str(e)}"
            )