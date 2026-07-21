from app.agents.report_agent import (
    ReportAgent
)


def main():

    curriculum_result = {
        "coverage_percentage": 75,
        "covered_topics": ["K-Means", "Applications"]
    }

    pedagogy_result = {
        "clarity_score": 8,
        "flow_score": 8
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

    audit_result = {
        "hallucination_risk": "Low"
    }

    agent = ReportAgent()

    result = agent.run(
        curriculum_result,
        pedagogy_result,
        visual_result,
        engagement_result,
        evidence_result,
        audit_result
    )

    print("\n===== FINAL REPORT =====\n")

    print(result)


if __name__ == "__main__":
    main()