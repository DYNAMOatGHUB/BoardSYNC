from app.models.schemas import (
    TeachingContext
)

from app.agents.curriculum_agent import (
    CurriculumAgent
)


def main():

    context = TeachingContext(

        faculty_name="Dr Priya",

        subject="Machine Learning",

        session_id="ML001",

        transcript="""
        Today we discussed K-Means clustering.

        We explained centroid assignment.

        Applications of K-Means were discussed.

        Python implementation was demonstrated.
        """,

        visual_summary="""
        Cluster diagram displayed.
        Python code shown.
        """,

        rag_context="""
        Introduction

        K-Means

        Applications

        Limitations
        """
    )

    agent = CurriculumAgent()

    result = agent.run(
        context
    )

    print("\n===== CURRICULUM RESULT =====\n")

    print(result)


if __name__ == "__main__":
    main()