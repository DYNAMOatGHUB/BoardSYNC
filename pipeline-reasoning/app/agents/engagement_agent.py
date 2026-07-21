import logging
from app.agents.base_agent import BaseAgent
from app.models.schemas import TeachingContext, EngagementResult
from app.prompts.engagement_prompt import ENGAGEMENT_PROMPT

logger = logging.getLogger(__name__)


class EngagementAgent(BaseAgent):
    """
    Classroom Engagement Evaluation Agent.
    Evaluates interaction, student participation, and discussion quality.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        super().__init__(model_name=model_name, temperature=temperature)

    def run(self, context: TeachingContext) -> EngagementResult:
        """
        Executes the engagement evaluation analysis.
        """
        self.logger.info(f"Running EngagementAgent for faculty: {context.faculty_name}")
        
        prompt = self.format_prompt(
            ENGAGEMENT_PROMPT,
            transcript=context.transcript
        )
        
        try:
            return self.get_structured_output(prompt, EngagementResult)
        except Exception as e:
            self.logger.error(f"EngagementAgent failed: {e}", exc_info=True)
            # Return a fallback EngagementResult
            return EngagementResult(
                interaction_level="Unknown",
                question_frequency="Unknown",
                participation_level="Unknown",
                discussion_quality="Unknown",
                summary=f"Error during engagement analysis: {str(e)}"
            )