from app.models.schemas import (
    TeachingContext
)

from app.agents.evidence_agent import (
    EvidenceAgent
)


def main():

    context = TeachingContext(

        faculty_name="Dr Priya",

        subject="Machine Learning",

        session_id="ML001",

        transcript="""
        Faculty explained K-Means.

        Customer segmentation example used.

        Students asked questions.

        Python code demonstrated.
        """,

        visual_summary="",

        rag_context=""
    )

    curriculum_result = {
        "coverage_percentage": 75
    }

    pedagogy_result = {
        "clarity_score": 8
    }

    visual_result = {
        "visual_effectiveness": "Good"
    }

    engagement_result = {
        "interaction_level": "High"
    }

    agent = EvidenceAgent()

    result = agent.run(
        context,
        curriculum_result,
        pedagogy_result,
        visual_result,
        engagement_result
    )

    print("\n===== EVIDENCE RESULT =====\n")

    print(result)


if __name__ == "__main__":
    main()