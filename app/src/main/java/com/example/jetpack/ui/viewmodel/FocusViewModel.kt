package com.example.jetpack.ui.viewmodel

import android.hardware.Sensor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jetpack.data.model.FocusSession
import com.example.jetpack.data.repository.SessionRepository
import com.example.jetpack.sensor.SensorRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class FocusStatus { IDLE, RUNNING, PAUSED, WAITING_FLIP }

data class FocusUiState(
    val subject: String = "",
    val status: FocusStatus = FocusStatus.IDLE,
    val elapsedSeconds: Long = 0,
    val distractions: Int = 0,
    val flipMode: Boolean = false,
    // Sensor cahaya
    val lux: Float = 0f,
    val lightAvailable: Boolean = true,
    // Sensor proximity
    val isNear: Boolean = false,
    val proximityCm: Float = 0f,
    val proximityAvailable: Boolean = true,
    // Accelerometer
    val movement: Float = 0f,
    val accelAvailable: Boolean = true
) {
    val isActive: Boolean get() = status != FocusStatus.IDLE

    val lightLabel: String
        get() = when {
            !lightAvailable -> "Sensor tidak tersedia"
            lux < 10f -> "Gelap"
            lux < 100f -> "Redup"
            lux < 300f -> "Cukup"
            lux < 1000f -> "Ideal untuk membaca"
            else -> "Sangat terang"
        }

    val lightAdvice: String
        get() = when {
            !lightAvailable -> "-"
            lux < 100f -> "Nyalakan lampu agar mata tidak cepat lelah."
            lux < 300f -> "Masih oke, tapi tambah cahaya sedikit lebih baik."
            lux < 1000f -> "Pencahayaan sudah pas untuk belajar."
            else -> "Terlalu silau, kurangi cahaya langsung."
        }
}

/**
 * ViewModel layar Fokus: menggabungkan data 3 sensor dengan logika timer,
 * lalu menyimpan hasil sesi ke SQLite lewat SessionRepository.
 */
class FocusViewModel(
    private val sessionRepository: SessionRepository,
    private val sensorRepository: SensorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FocusUiState(
            lightAvailable = sensorRepository.isAvailable(Sensor.TYPE_LIGHT),
            proximityAvailable = sensorRepository.isAvailable(Sensor.TYPE_PROXIMITY),
            accelAvailable = sensorRepository.isAvailable(Sensor.TYPE_ACCELEROMETER)
        )
    )
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var sessionStart = 0L
    private var luxSum = 0.0
    private var luxCount = 0
    private var lastAccel: FloatArray? = null
    private var lastDistractionAt = 0L
    private var manuallyPaused = false

    companion object {
        private const val MOVEMENT_THRESHOLD = 2.5f      // m/s^2 perubahan antar pembacaan
        private const val DISTRACTION_COOLDOWN_MS = 3000L
    }

    init {
        observeLight()
        observeProximity()
        observeAccelerometer()
        startTicker()
    }

    // ---------------- Sensor ----------------

    private fun observeLight() = viewModelScope.launch {
        sensorRepository.observe(Sensor.TYPE_LIGHT).collect { values ->
            val lux = values[0]
            if (_uiState.value.status == FocusStatus.RUNNING) {
                luxSum += lux
                luxCount++
            }
            _uiState.update { it.copy(lux = lux) }
        }
    }

    private fun observeProximity() = viewModelScope.launch {
        val maxRange = sensorRepository.proximityMaxRange
        sensorRepository.observe(Sensor.TYPE_PROXIMITY).collect { values ->
            val distance = values[0]
            val near = distance < maxRange && distance < 5f
            val wasNear = _uiState.value.isNear
            _uiState.update { it.copy(isNear = near, proximityCm = distance) }

            // HP diangkat saat mode telungkup berjalan -> dihitung sebagai gangguan
            if (wasNear && !near && _uiState.value.flipMode &&
                _uiState.value.status == FocusStatus.RUNNING
            ) {
                registerDistraction()
            }
            updateStatus()
        }
    }

    private fun observeAccelerometer() = viewModelScope.launch {
        sensorRepository.observe(Sensor.TYPE_ACCELEROMETER).collect { v ->
            val prev = lastAccel
            lastAccel = v
            if (prev == null) return@collect
            val dx = v[0] - prev[0]
            val dy = v[1] - prev[1]
            val dz = v[2] - prev[2]
            val movement = sqrt(dx * dx + dy * dy + dz * dz)
            _uiState.update { it.copy(movement = movement) }

            if (movement > MOVEMENT_THRESHOLD &&
                _uiState.value.status == FocusStatus.RUNNING
            ) {
                registerDistraction()
            }
        }
    }

    private fun registerDistraction() {
        val now = System.currentTimeMillis()
        if (now - lastDistractionAt < DISTRACTION_COOLDOWN_MS) return
        lastDistractionAt = now
        _uiState.update { it.copy(distractions = it.distractions + 1) }
    }

    // ---------------- Timer ----------------

    private fun startTicker() = viewModelScope.launch {
        while (true) {
            delay(1000)
            if (_uiState.value.status == FocusStatus.RUNNING) {
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    /** Menentukan status RUNNING / PAUSED / WAITING_FLIP dari kondisi saat ini. */
    private fun updateStatus() {
        _uiState.update { s ->
            if (s.status == FocusStatus.IDLE) return@update s
            val newStatus = when {
                manuallyPaused -> FocusStatus.PAUSED
                s.flipMode && s.proximityAvailable && !s.isNear -> FocusStatus.WAITING_FLIP
                else -> FocusStatus.RUNNING
            }
            // Beri jeda 3 detik saat mulai/lanjut agar gerakan meletakkan HP tidak dihitung
            if (newStatus == FocusStatus.RUNNING && s.status != FocusStatus.RUNNING) {
                lastDistractionAt = System.currentTimeMillis()
            }
            s.copy(status = newStatus)
        }
    }

    // ---------------- Aksi dari UI ----------------

    fun onSubjectChange(value: String) = _uiState.update { it.copy(subject = value) }

    fun onFlipModeChange(enabled: Boolean) {
        _uiState.update { it.copy(flipMode = enabled) }
        updateStatus()
    }

    fun start() {
        sessionStart = System.currentTimeMillis()
        luxSum = 0.0
        luxCount = 0
        manuallyPaused = false
        lastDistractionAt = System.currentTimeMillis() // abaikan getaran saat menekan tombol
        _uiState.update {
            it.copy(status = FocusStatus.RUNNING, elapsedSeconds = 0, distractions = 0)
        }
        updateStatus()
    }

    fun togglePause() {
        manuallyPaused = !manuallyPaused
        lastDistractionAt = System.currentTimeMillis()
        updateStatus()
    }

    fun finishAndSave() {
        val s = _uiState.value
        if (!s.isActive) return
        if (s.elapsedSeconds < 1) {
            reset()
            _messages.tryEmit("Sesi terlalu singkat, tidak disimpan.")
            return
        }
        val session = FocusSession(
            subject = s.subject.ifBlank { "Belajar" },
            startTime = sessionStart,
            durationSeconds = s.elapsedSeconds,
            distractions = s.distractions,
            avgLux = if (luxCount > 0) (luxSum / luxCount).toFloat() else s.lux
        )
        viewModelScope.launch {
            sessionRepository.addSession(session)
            _messages.emit("Sesi disimpan! Skor fokus: ${session.focusScore}")
        }
        reset()
    }

    fun reset() {
        manuallyPaused = false
        _uiState.update {
            it.copy(status = FocusStatus.IDLE, elapsedSeconds = 0, distractions = 0)
        }
    }
}
