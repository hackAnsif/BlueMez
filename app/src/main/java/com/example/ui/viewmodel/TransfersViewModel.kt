package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AttachmentEntity
import com.example.repository.ChatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransfersViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {

    val allAttachments: StateFlow<List<AttachmentEntity>> = chatRepository.allAttachmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTransfers: StateFlow<List<AttachmentEntity>> = chatRepository.activeTransfersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancelTransfer(attachmentId: String) {
        viewModelScope.launch {
            chatRepository.transferManager.cancelTransfer(attachmentId, "Cancelled by user")
        }
    }
}

class TransfersViewModelFactory(
    private val chatRepository: ChatRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TransfersViewModel(chatRepository) as T
    }
}
