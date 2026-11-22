package com.example.jetpack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jetpack.data.model.formatDuration
import com.example.jetpack.ui.viewmodel.FocusStatus
import com.example.jetpack.ui.viewmodel.FocusUiState

@Composable
fun FocusScreen(
    state: FocusUiState,
    onSubjectChange: (String) -> Unit,
    onFlipModeChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("FokusKu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Timer belajar yang dibantu sensor HP",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = state.subject,
            onValueChange = onSubjectChange,
            label = { Text("Mata pelajaran / kegiatan") },
            placeholder = { Text("contoh: Pemrograman Android") },
            singleLine = true,
            enabled = !state.isActive,
            modifier = Modifier.fillMaxWidth()
        )

        TimerCard(state)

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Mode Telungkup", fontWeight = FontWeight.SemiBold)
                    Text(
                        if (state.proximityAvailable)
                            "Timer hanya berjalan saat HP ditelungkupkan (sensor proximity tertutup)."
                        else "Sensor proximity tidak tersedia di perangkat ini.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = state.flipMode,
                    onCheckedChange = onFlipModeChange,
                    enabled = state.proximityAvailable
                )
            }
        }

        if (!state.isActive) {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text("Mulai Fokus")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onTogglePause, modifier = Modifier.weight(1f)) {
                    Text(if (state.status == FocusStatus.PAUSED) "Lanjut" else "Jeda")
                }
                Button(onClick = onFinish, modifier = Modifier.weight(1f)) {
                    Text("Selesai & Simpan")
                }
            }
            OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                Text("Batalkan Sesi")
            }
        }

        Text("Data Sensor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        SensorCard(
            title = "Cahaya Ruangan",
            category = "Environment Sensor • TYPE_LIGHT",
            value = if (state.lightAvailable) "%.0f lux".format(state.lux) else "-",
            description = "${state.lightLabel}. ${state.lightAdvice}",
            progress = (state.lux / 1000f).coerceIn(0f, 1f)
        )
        SensorCard(
            title = "Gerakan HP",
            category = "Motion Sensor • TYPE_ACCELEROMETER",
            value = if (state.accelAvailable) "%.2f m/s²".format(state.movement) else "-",
            description = "Gangguan terdeteksi sesi ini: ${state.distractions} kali. " +
                "HP yang diangkat/digoyang saat fokus dihitung sebagai gangguan.",
            progress = (state.movement / 5f).coerceIn(0f, 1f)
        )
        SensorCard(
            title = "Posisi HP",
            category = "Position Sensor • TYPE_PROXIMITY",
            value = if (state.proximityAvailable) {
                if (state.isNear) "Tertutup" else "Terbuka"
            } else "-",
            description = if (state.proximityAvailable)
                "Jarak terbaca: %.1f cm. Telungkupkan HP di meja untuk menutup sensor.".format(state.proximityCm)
            else "Sensor tidak tersedia.",
            progress = if (state.isNear) 1f else 0f
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TimerCard(state: FocusUiState) {
    val (statusText, color) = when (state.status) {
        FocusStatus.IDLE -> "Siap memulai" to MaterialTheme.colorScheme.onSurfaceVariant
        FocusStatus.RUNNING -> "Sedang fokus…" to MaterialTheme.colorScheme.primary
        FocusStatus.PAUSED -> "Dijeda" to MaterialTheme.colorScheme.tertiary
        FocusStatus.WAITING_FLIP -> "Telungkupkan HP untuk melanjutkan" to MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                formatDuration(state.elapsedSeconds),
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(statusText, color = color, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(
                "Gangguan: ${state.distractions}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SensorCard(
    title: String,
    category: String,
    value: String,
    description: String,
    progress: Float
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(
                        category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
