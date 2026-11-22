package com.example.jetpack.data.repository

import com.example.jetpack.data.local.FocusDatabaseHelper
import com.example.jetpack.data.model.FocusSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository: satu-satunya pintu ViewModel ke database SQLite.
 * Setiap perubahan data memuat ulang daftar sesi agar UI ikut ter-update.
 */
class SessionRepository(private val db: FocusDatabaseHelper) {

    private val _sessions = MutableStateFlow<List<FocusSession>>(emptyList())
    val sessions: StateFlow<List<FocusSession>> = _sessions.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _sessions.value = db.getAllSessions()
    }

    suspend fun addSession(session: FocusSession) = withContext(Dispatchers.IO) {
        db.insertSession(session)
        refresh()
    }

    suspend fun updateSession(session: FocusSession) = withContext(Dispatchers.IO) {
        db.updateSession(session)
        refresh()
    }

    suspend fun deleteSession(id: Long) = withContext(Dispatchers.IO) {
        db.deleteSession(id)
        refresh()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        db.deleteAllSessions()
        refresh()
    }
}
