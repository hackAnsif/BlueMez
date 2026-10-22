package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object NearbyDevices : Screen("nearby_devices")
    object Chat : Screen("chat/{peerId}") {
        fun createRoute(peerId: String): String = "chat/$peerId"
    }
    object Transfers : Screen("transfers")
    object MediaViewer : Screen("media_viewer/{attachmentId}") {
        fun createRoute(attachmentId: String): String = "media_viewer/$attachmentId"
    }
    object Settings : Screen("settings")
}
