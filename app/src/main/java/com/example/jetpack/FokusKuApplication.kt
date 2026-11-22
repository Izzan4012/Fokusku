package com.example.jetpack

import android.app.Application
import com.example.jetpack.data.local.FocusDatabaseHelper
import com.example.jetpack.data.repository.SessionRepository
import com.example.jetpack.sensor.SensorRepository

/** Wadah dependensi sederhana (manual dependency injection). */
class FokusKuApplication : Application() {
    val sessionRepository: SessionRepository by lazy {
        SessionRepository(FocusDatabaseHelper(this))
    }
    val sensorRepository: SensorRepository by lazy {
        SensorRepository(this)
    }
}
