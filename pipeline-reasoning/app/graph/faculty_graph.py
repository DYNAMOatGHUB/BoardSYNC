from langgraph.graph import StateGraph
from langgraph.graph import END

from app.models.state import AgentState

from app.agents.curriculum_agent import CurriculumAgent
from app.agents.pedagogy_agent import PedagogyAgent
from app.agents.visual_learning_agent import VisualLearningAgent
from app.agents.engagement_agent import EngagementAgent
from app.agents.evidence_agent import EvidenceAgent
from app.agents.auditor_agent import AuditorAgent
from app.agents.report_agent import ReportAgent


curriculum_agent = CurriculumAgent()
pedagogy_agent = PedagogyAgent()
visual_agent = VisualLearningAgent()
engagement_agent = EngagementAgent()
evidence_agent = EvidenceAgent()
auditor_agent = AuditorAgent()
report_agent = ReportAgent()


def curriculum_node(state: AgentState):

    state.curriculum_result = curriculum_agent.run(
        state.context
    )

    return state


def pedagogy_node(state: AgentState):

    state.pedagogy_result = pedagogy_agent.run(
        state.context
    )

    return state


def visual_node(state: AgentState):

    state.visual_result = visual_agent.run(
        state.context
    )

    return state


def engagement_node(state: AgentState):

    state.engagement_result = engagement_agent.run(
        state.context
    )

    return state


def evidence_node(state: AgentState):

    state.evidence_result = evidence_agent.run(
        state.context,
        state.curriculum_result,
        state.pedagogy_result,
        state.visual_result,
        state.engagement_result
    )

    return state


def auditor_node(state: AgentState):

    state.audit_result = auditor_agent.run(
        state.curriculum_result,
        state.pedagogy_result,
        state.visual_result,
        state.engagement_result,
        state.evidence_result
    )

    return state


def report_node(state: AgentState):

    state.final_report = report_agent.run(
        state.curriculum_result,
        state.pedagogy_result,
        state.visual_result,
        state.engagement_result,
        state.evidence_result,
        state.audit_result
    )

    return state


graph = StateGraph(
    AgentState
)

graph.add_node(
    "curriculum",
    curriculum_node
)

graph.add_node(
    "pedagogy",
    pedagogy_node
)

graph.add_node(
    "visual",
    visual_node
)

graph.add_node(
    "engagement",
    engagement_node
)

graph.add_node(
    "evidence",
    evidence_node
)

graph.add_node(
    "auditor",
    auditor_node
)

graph.add_node(
    "report",
    report_node
)

graph.set_entry_point(
    "curriculum"
)

graph.add_edge(
    "curriculum",
    "pedagogy"
)

graph.add_edge(
    "pedagogy",
    "visual"
)

graph.add_edge(
    "visual",
    "engagement"
)

graph.add_edge(
    "engagement",
    "evidence"
)

graph.add_edge(
    "evidence",
    "auditor"
)

graph.add_edge(
    "auditor",
    "report"
)

graph.add_edge(
    "report",
    END
)

faculty_graph = graph.compile()