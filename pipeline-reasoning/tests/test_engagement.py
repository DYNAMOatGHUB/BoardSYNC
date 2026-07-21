from app.models.schemas import (
    TeachingContext
)

from app.agents.engagement_agent import (
    EngagementAgent
)


def main():

    context = TeachingContext(

        faculty_name="Dr Priya",

        subject="Machine Learning",

        session_id="ML001",

        transcript="""
        Faculty asked students:

        What is clustering?

        Multiple students responded.

        A discussion followed about customer segmentation.

        Students asked questions about centroid selection.

        Faculty answered all questions.
        """,

        visual_summary="",

        rag_context=""
    )

    agent = EngagementAgent()

    result = agent.run(
        context
    )

    print("\n===== ENGAGEMENT RESULT =====\n")

    print(result)


if __name__ == "__main__":
    main()