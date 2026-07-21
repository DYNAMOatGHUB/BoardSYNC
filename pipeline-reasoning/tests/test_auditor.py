from app.agents.auditor_agent import (
    AuditorAgent
)


def main():

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

    evidence_result = {
        "evidence_points": [
            "Customer segmentation example used"
        ]
    }

    agent = AuditorAgent()

    result = agent.run(
        curriculum_result,
        pedagogy_result,
        visual_result,
        engagement_result,
        evidence_result
    )

    print("\n===== AUDITOR RESULT =====\n")

    print(result)


if __name__ == "__main__":
    main()