package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DiscoveryViewModel
import com.example.ui.viewmodel.HomeViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TransfersViewModel

enum class MainNavTab(val title: String) {
    CHATS("Chats"),
    CONTACTS("Contacts"),
    TRANSFERS("Transfers"),
    SETTINGS("Settings")
}

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    discoveryViewModel: DiscoveryViewModel,
    transfersViewModel: TransfersViewModel,
    settingsViewModel: SettingsViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToMediaViewer: (String) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats") },
                    label = { Text("Chats", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_chats")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = "Contacts") },
                    label = { Text("Contacts", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_contacts")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.SwapVert, contentDescription = "Transfers") },
                    label = { Text("Transfers", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_transfers")
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 12.sp) },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToContacts = { selectedTabIndex = 1 }
                )
                1 -> ContactsScreen(
                    viewModel = discoveryViewModel,
                    onNavigateToChat = onNavigateToChat
                )
                2 -> TransfersScreen(
                    viewModel = transfersViewModel,
                    onNavigateBack = null,
                    onNavigateToMediaViewer = onNavigateToMediaViewer
                )
                3 -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = null
                )
            }
        }
    }
}
