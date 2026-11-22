package com.example.jetpack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.jetpack.data.model.FocusSession
import com.example.jetpack.ui.viewmodel.HistoryUiState

@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onUpdate: (FocusSession) -> Unit,
    onDelete: (FocusSession) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf<FocusSession?>(null) }
    var deleting by remember { mutableStateOf<FocusSession?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Riwayat Sesi",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (state.sessions.isNotEmpty()) {
                    TextButton(onClick = { confirmDeleteAll = true }) { Text("Hapus semua") }
                }
            }
        }
        item { SummaryCard(state) }

        if (state.sessions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Belum ada sesi tersimpan.\nMulai sesi fokus di tab Fokus.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(state.sessions, key = { it.id }) { session ->
                SessionCard(
                    session = session,
                    onEdit = { editing = session },
                    onDelete = { deleting = session }
                )
            }
        }
    }

    editing?.let { session ->
        EditSessionDialog(
            session = session,
            onDismiss = { editing = null },
            onSave = {
                onUpdate(it)
                editing = null
            }
        )
    }

    deleting?.let { session ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Hapus sesi?") },
            text = { Text("Sesi \"${session.subject}\" (${session.formattedDate}) akan dihapus.") },
            confirmButton = {
                TextButton(onClick = { onDelete(session); deleting = null }) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Batal") } }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Hapus semua riwayat?") },
            text = { Text("Semua sesi akan dihapus dari database dan tidak bisa dikembalikan.") },
            confirmButton = {
                TextButton(onClick = { onDeleteAll(); confirmDeleteAll = false }) { Text("Hapus semua") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun SummaryCard(state: HistoryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            SummaryItem("Sesi", state.totalSessions.toString())
            SummaryItem("Total menit", state.totalMinutes.toString())
            SummaryItem("Rata-rata skor", state.averageScore.toString())
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SessionCard(session: FocusSession, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(session.subject, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        session.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ScoreBadge(session.focusScore)
            }
            Text("Durasi: ${session.formattedDuration}  •  Gangguan: ${session.distractions}")
            Text("Rata-rata cahaya: %.0f lux".format(session.avgLux), style = MaterialTheme.typography.bodySmall)
            if (session.note.isNotBlank()) {
                Text("Catatan: ${session.note}", style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onDelete) { Text("Hapus") }
            }
        }
    }
}

@Composable
private fun ScoreBadge(score: Int) {
    val color = when {
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 50 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$score", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text("skor", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EditSessionDialog(
    session: FocusSession,
    onDismiss: () -> Unit,
    onSave: (FocusSession) -> Unit
) {
    var subject by remember { mutableStateOf(session.subject) }
    var note by remember { mutableStateOf(session.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Sesi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Mata pelajaran") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(session.copy(subject = subject.ifBlank { session.subject }, note = note.trim()))
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
