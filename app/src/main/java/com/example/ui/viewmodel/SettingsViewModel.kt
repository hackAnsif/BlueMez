package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val displayName: StateFlow<String> = chatRepository.preferencesManager.displayNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "BlueMez User")

    val themeMode: StateFlow<String> = chatRepository.preferencesManager.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val myPeerId: StateFlow<String> = chatRepository.preferencesManager.myPeerIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val autoReconnect: StateFlow<Boolean> = chatRepository.preferencesManager.autoReconnectFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val readReceipts: StateFlow<Boolean> = chatRepository.preferencesManager.readReceiptsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val maxImageMb: StateFlow<Int> = chatRepository.preferencesManager.maxImageMbFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 25)

    val maxVideoMb: StateFlow<Int> = chatRepository.preferencesManager.maxVideoMbFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            chatRepository.preferencesManager.setDisplayName(name)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            chatRepository.preferencesManager.setThemeMode(mode)
        }
    }

    fun setAutoReconnect(enabled: Boolean) {
        viewModelScope.launch {
            chatRepository.preferencesManager.setAutoReconnect(enabled)
        }
    }

    fun setReadReceipts(enabled: Boolean) {
        viewModelScope.launch {
            chatRepository.preferencesManager.setReadReceipts(enabled)
        }
    }

    fun setMaxImageMb(mb: Int) {
        viewModelScope.launch {
            chatRepository.preferencesManager.setMaxImageMb(mb)
        }
    }

    fun setMaxVideoMb(mb: Int) {
        viewModelScope.launch {
            chatRepository.preferencesManager.setMaxVideoMb(mb)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatRepository.clearAllData()
        }
    }
}

class SettingsViewModelFactory(
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(chatRepository) as T
    }
}
