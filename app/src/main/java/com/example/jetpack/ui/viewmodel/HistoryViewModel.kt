package com.example.jetpack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpack.data.model.FocusSession
import com.example.jetpack.data.repository.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val sessions: List<FocusSession> = emptyList()
) {
    val totalSessions: Int get() = sessions.size
    val totalMinutes: Long get() = sessions.sumOf { it.durationSeconds } / 60
    val averageScore: Int
        get() = if (sessions.isEmpty()) 0 else sessions.map { it.focusScore }.average().toInt()
}

/**
 * ViewModel layar Riwayat: membaca, mengubah, dan menghapus data sesi di SQLite.
 */
class HistoryViewModel(private val repository: SessionRepository) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.sessions
        .map { HistoryUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    init {
        viewModelScope.launch { repository.refresh() }
    }

    fun update(session: FocusSession) = viewModelScope.launch {
        repository.updateSession(session)
    }

    fun delete(session: FocusSession) = viewModelScope.launch {
        repository.deleteSession(session.id)
    }

    fun deleteAll() = viewModelScope.launch { repository.deleteAll() }
}
