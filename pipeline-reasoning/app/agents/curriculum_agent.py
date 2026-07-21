import logging
from app.agents.base_agent import BaseAgent
from app.models.schemas import TeachingContext, CurriculumResult
from app.prompts.curriculum_prompt import CURRICULUM_PROMPT

logger = logging.getLogger(__name__)


class CurriculumAgent(BaseAgent):
    """
    Curriculum Evaluation Agent.
    Evaluates curriculum coverage based on lecture transcript and RAG context.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        super().__init__(model_name=model_name, temperature=temperature)

    def run(self, context: TeachingContext) -> CurriculumResult:
        """
        Executes the curriculum evaluation analysis.
        """
        self.logger.info(f"Running CurriculumAgent for faculty: {context.faculty_name}")
        
        prompt = self.format_prompt(
            CURRICULUM_PROMPT,
            rag_context=context.rag_context,
            transcript=context.transcript
        )
        
        try:
            return self.get_structured_output(prompt, CurriculumResult)
        except Exception as e:
            self.logger.error(f"CurriculumAgent failed: {e}", exc_info=True)
            # Return a fallback CurriculumResult
            return CurriculumResult(
                coverage_percentage=0,
                covered_topics=[],
                missing_topics=[],
                extra_topics=[],
                summary=f"Error during curriculum analysis: {str(e)}"
            )