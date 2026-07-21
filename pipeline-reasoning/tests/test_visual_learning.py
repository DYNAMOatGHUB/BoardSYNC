from app.models.schemas import (
    TeachingContext
)

from app.agents.visual_learning_agent import (
    VisualLearningAgent
)


def main():

    context = TeachingContext(

        faculty_name="Dr Priya",

        subject="Machine Learning",

        session_id="ML001",

        transcript="""
        Today we discussed K-Means clustering.

        A cluster diagram was shown.

        Python implementation was demonstrated.

        Students were shown output graphs.
        """,

        visual_summary="""
        PPT slides displayed.

        Cluster diagram shown.

        Python code shown.

        Graph visualization displayed.
        """,

        rag_context=""
    )

    agent = VisualLearningAgent()

    result = agent.run(
        context
    )

    print("\n===== VISUAL LEARNING RESULT =====\n")

    print(result)


if __name__ == "__main__":
    main()