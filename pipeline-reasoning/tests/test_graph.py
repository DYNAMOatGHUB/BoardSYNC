from app.models.schemas import (
    TeachingContext
)

from app.models.state import (
    AgentState
)

from app.graph.faculty_graph import (
    faculty_graph
)


context = TeachingContext(

    faculty_name="Dr Priya",

    subject="Machine Learning",

    session_id="ML001",

    transcript="""
    Today we discussed K-Means clustering.

    Customer segmentation example explained.

    Python implementation demonstrated.

    Students asked questions.
    """,

    visual_summary="""
    PPT shown.

    Diagram shown.

    Python code shown.
    """,

    rag_context="""
    Introduction

    K-Means

    Applications

    Limitations
    """
)

state = AgentState(
    context=context
)

result = faculty_graph.invoke(
    state
)

print("\n===== FINAL REPORT =====\n")

print(
    result["final_report"]
)