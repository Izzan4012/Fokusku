package com.example.jetpack.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Membungkus SensorManager menjadi Flow agar mudah dipakai di ViewModel.
 *
 * Sensor yang dipakai:
 *  - TYPE_ACCELEROMETER  (Motion Sensor)      -> deteksi HP digerakkan/diangkat (gangguan)
 *  - TYPE_PROXIMITY      (Position Sensor)    -> deteksi HP ditelungkupkan (mode fokus)
 *  - TYPE_LIGHT          (Environment Sensor) -> ukur intensitas cahaya ruangan (lux)
 */
class SensorRepository(context: Context) {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun isAvailable(type: Int): Boolean = sensorManager.getDefaultSensor(type) != null

    /** Jarak maksimum sensor proximity (cm). Nilai di bawahnya berarti "dekat". */
    val proximityMaxRange: Float
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.maximumRange ?: 5f

    /** Mengalirkan nilai sensor (event.values) selama Flow dikoleksi. */
    fun observe(
        type: Int,
        delay: Int = SensorManager.SENSOR_DELAY_UI
    ): Flow<FloatArray> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(type)
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values.clone())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, delay)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
