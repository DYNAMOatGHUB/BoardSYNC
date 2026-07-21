import os
import logging
from abc import ABC, abstractmethod
from typing import Any, Type, TypeVar
from pydantic import BaseModel
from langchain_ollama import ChatOllama
from langchain_core.prompts import PromptTemplate
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type

# Set up logging format and basic config
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger(__name__)

T = TypeVar("T", bound=BaseModel)


class BaseAgent(ABC):
    """
    Production-grade base class for all evaluation agents.
    Provides structured output, retry handling, exception handling,
    and safe prompt formatting.
    """

    def __init__(self, model_name: str | None = None, temperature: float = 0.2):
        # Default to qwen3:8b, but allow qwen3:14b or customization via env var
        self.model_name = model_name or os.getenv("OLLAMA_MODEL", "qwen3:8b")
        self.temperature = temperature
        
        self.logger = logging.getLogger(self.__class__.__name__)
        self.logger.info(f"Initializing ChatOllama with model={self.model_name}, temperature={self.temperature}")

        try:
            self.llm = ChatOllama(
                model=self.model_name,
                temperature=self.temperature
            )
        except Exception as e:
            self.logger.critical(f"Failed to initialize ChatOllama model: {e}", exc_info=True)
            raise e

    @abstractmethod
    def run(self, *args: Any, **kwargs: Any) -> Any:
        """
        Execute agent logic. Must be implemented by subclasses.
        """
        pass

    def format_prompt(self, template: str, **kwargs: Any) -> str:
        """
        Safely formats prompt templates by converting Pydantic models or dicts
        to formatted JSON strings, preventing parsing errors and keeping LLM inputs structured.
        """
        formatted_kwargs = {}
        for key, value in kwargs.items():
            if isinstance(value, BaseModel):
                formatted_kwargs[key] = value.model_dump_json(indent=2)
            elif isinstance(value, dict):
                import json
                formatted_kwargs[key] = json.dumps(value, indent=2)
            elif value is None:
                formatted_kwargs[key] = "None"
            else:
                formatted_kwargs[key] = str(value)

        try:
            prompt_template = PromptTemplate.from_template(template)
            return prompt_template.format(**formatted_kwargs)
        except Exception as e:
            self.logger.error(f"Error formatting prompt template safely: {e}", exc_info=True)
            raise e

    def get_structured_output(self, prompt: str, schema: Type[T]) -> T:
        """
        Invokes LLM and returns output structured exactly as the specified Pydantic schema.
        Includes built-in retry handling and output validation.
        """
        structured_llm = self.llm.with_structured_output(schema)

        @retry(
            stop=stop_after_attempt(3),
            wait=wait_exponential(multiplier=1, min=2, max=10),
            retry=retry_if_exception_type(Exception),
            before_sleep=lambda retry_state: self.logger.warning(
                f"LLM structured invocation failed with: {retry_state.outcome.exception()}. "
                f"Retrying attempt {retry_state.attempt_number}..."
            ),
            reraise=True
        )
        def _invoke() -> T:
            self.logger.info(f"Invoking LLM for structured output schema: {schema.__name__}")
            result = structured_llm.invoke(prompt)
            if not isinstance(result, schema):
                raise ValueError(
                    f"LLM returned an object of type {type(result).__name__}, expected {schema.__name__}"
                )
            return result

        try:
            return _invoke()
        except Exception as e:
            self.logger.error(f"Failed to get structured output from LLM after retries: {e}", exc_info=True)
            raise e