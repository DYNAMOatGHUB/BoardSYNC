package com.edge.smartboard.navigation

sealed class Screen(val route: String) {
    object Login       : Screen("login")
    object Dashboard   : Screen("dashboard")
    object Capture     : Screen("capture")
    object Audio       : Screen("audio")
    object Session     : Screen("session/{sessionId}") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
    object Upload      : Screen("upload/{sessionId}") {
        fun createRoute(sessionId: String) = "upload/$sessionId"
    }
    object AiStatus    : Screen("ai_status")
    object LiveProcess : Screen("live_process/{sessionId}") {
        fun createRoute(sessionId: String) = "live_process/$sessionId"
    }
    object Reports     : Screen("reports/{sessionId}") {
        fun createRoute(sessionId: String) = "reports/$sessionId"
    }
    object History     : Screen("history")
    object Notifications: Screen("notifications")
    object Settings    : Screen("settings")
    object Admin          : Screen("admin")
    object TeacherAnalysis: Screen("teacher_analysis")
}
