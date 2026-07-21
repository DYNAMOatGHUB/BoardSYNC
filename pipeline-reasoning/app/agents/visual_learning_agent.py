import logging
from app.agents.base_agent import BaseAgent
from app.models.schemas import TeachingContext, VisualLearningResult
from app.prompts.visual_learning_prompt import VISUAL_LEARNING_PROMPT

logger = logging.getLogger(__name__)


class VisualLearningAgent(BaseAgent):
    """
    Visual Learning Evaluation Agent.
    Evaluates slides, diagrams, formulas, code, and overall visual effectiveness.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        super().__init__(model_name=model_name, temperature=temperature)

    def run(self, context: TeachingContext) -> VisualLearningResult:
        """
        Executes the visual learning evaluation analysis.
        """
        self.logger.info(f"Running VisualLearningAgent for faculty: {context.faculty_name}")
        
        prompt = self.format_prompt(
            VISUAL_LEARNING_PROMPT,
            transcript=context.transcript,
            visual_summary=context.visual_summary
        )
        
        try:
            return self.get_structured_output(prompt, VisualLearningResult)
        except Exception as e:
            self.logger.error(f"VisualLearningAgent failed: {e}", exc_info=True)
            # Return a fallback VisualLearningResult
            return VisualLearningResult(
                ppt_usage="Unknown",
                diagram_usage="Unknown",
                formula_usage="Unknown",
                code_demonstration="Unknown",
                visual_effectiveness="Unknown",
                summary=f"Error during visual learning analysis: {str(e)}"
            )