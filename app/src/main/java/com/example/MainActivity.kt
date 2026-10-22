package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.NearbyDevicesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransfersScreen
import com.example.ui.theme.BlueMezTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory
import com.example.ui.viewmodel.DiscoveryViewModel
import com.example.ui.viewmodel.DiscoveryViewModelFactory
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.HomeViewModelFactory
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.SettingsViewModelFactory
import com.example.ui.viewmodel.TransfersViewModel
import com.example.ui.viewmodel.TransfersViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BlueMezApp

        setContent {
            BlueMezTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlueMezNavigationApp(app = app)
                }
            }
        }
    }
}

@Composable
fun BlueMezNavigationApp(app: BlueMezApp) {
    val navController = rememberNavController()
    val onboardingDone by app.preferencesManager.onboardingDoneFlow.collectAsState(initial = false)

    val startDestination = if (onboardingDone) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModelFactory(
                    chatRepository = app.chatRepository,
                    bluetoothManager = app.bluetoothManager
                )
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToDiscovery = { navController.navigate(Screen.NearbyDevices.route) },
                onNavigateToChat = { peerId -> navController.navigate(Screen.Chat.createRoute(peerId)) },
                onNavigateToTransfers = { navController.navigate(Screen.Transfers.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.NearbyDevices.route) {
            val discoveryViewModel: DiscoveryViewModel = viewModel(
                factory = DiscoveryViewModelFactory(
                    chatRepository = app.chatRepository,
                    bluetoothManager = app.bluetoothManager
                )
            )
            NearbyDevicesScreen(
                viewModel = discoveryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onConnectedNavigateToChat = { peerId ->
                    navController.navigate(Screen.Chat.createRoute(peerId)) {
                        popUpTo(Screen.NearbyDevices.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("peerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            val chatViewModel: ChatViewModel = viewModel(
                key = peerId,
                factory = ChatViewModelFactory(
                    peerId = peerId,
                    chatRepository = app.chatRepository,
                    bluetoothManager = app.bluetoothManager
                )
            )
            ChatScreen(
                viewModel = chatViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMediaViewer = { attachmentId ->
                    navController.navigate(Screen.MediaViewer.createRoute(attachmentId))
                }
            )
        }

        composable(Screen.Transfers.route) {
            val transfersViewModel: TransfersViewModel = viewModel(
                factory = TransfersViewModelFactory(chatRepository = app.chatRepository)
            )
            TransfersScreen(
                viewModel = transfersViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMediaViewer = { attachmentId ->
                    navController.navigate(Screen.MediaViewer.createRoute(attachmentId))
                }
            )
        }

        composable(
            route = Screen.MediaViewer.route,
            arguments = listOf(navArgument("attachmentId") { type = NavType.StringType })
        ) { backStackEntry ->
            val attachmentId = backStackEntry.arguments?.getString("attachmentId") ?: ""
            MediaViewerScreen(
                attachmentId = attachmentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(chatRepository = app.chatRepository)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
