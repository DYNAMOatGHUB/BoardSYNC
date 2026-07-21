from app.models.schemas import (
    TeachingContext
)

from app.agents.pedagogy_agent import (
    PedagogyAgent
)


def main():

    context = TeachingContext(

        faculty_name="Dr Priya",

        subject="Machine Learning",

        session_id="ML001",

        transcript="""
        Today we discussed K-Means clustering.

        First we introduced clustering.

        Then we explained centroids with examples.

        A real-world customer segmentation example was shown.

        Finally we demonstrated Python implementation.
        """,

        visual_summary="",

        rag_context=""
    )

    agent = PedagogyAgent()

    result = agent.run(
        context
    )

    print("\n===== PEDAGOGY RESULT =====\n")

    print(result)


if __name__ == "__main__":
    main()