package com.example.jetpack.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model: satu sesi belajar/fokus yang disimpan di SQLite.
 */
data class FocusSession(
    val id: Long = 0,
    val subject: String,
    val startTime: Long,
    val durationSeconds: Long,
    val distractions: Int,
    val avgLux: Float,
    val note: String = ""
) {
    /** Skor fokus 0..100: dikurangi tiap gangguan gerakan dan bila cahaya terlalu redup. */
    val focusScore: Int
        get() {
            var score = 100 - distractions * 10
            if (avgLux in 0f..99f) score -= 10
            return score.coerceIn(0, 100)
        }

    val formattedDate: String
        get() = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
            .format(Date(startTime))

    val formattedDuration: String
        get() = formatDuration(durationSeconds)
}

fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
