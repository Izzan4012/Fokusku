package com.example.jetpack.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.jetpack.data.model.FocusSession

/**
 * Database SQLite (SQLiteOpenHelper) untuk menyimpan riwayat sesi fokus.
 */
class FocusDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "fokusku.db"
        const val DATABASE_VERSION = 1

        const val TABLE_SESSIONS = "sessions"
        const val COL_ID = "id"
        const val COL_SUBJECT = "subject"
        const val COL_START_TIME = "start_time"
        const val COL_DURATION = "duration_seconds"
        const val COL_DISTRACTIONS = "distractions"
        const val COL_AVG_LUX = "avg_lux"
        const val COL_NOTE = "note"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SESSIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_SUBJECT TEXT NOT NULL,
                $COL_START_TIME INTEGER NOT NULL,
                $COL_DURATION INTEGER NOT NULL,
                $COL_DISTRACTIONS INTEGER NOT NULL,
                $COL_AVG_LUX REAL NOT NULL,
                $COL_NOTE TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SESSIONS")
        onCreate(db)
    }

    // ---------- CREATE ----------
    fun insertSession(session: FocusSession): Long =
        writableDatabase.insert(TABLE_SESSIONS, null, session.toContentValues())

    // ---------- READ ----------
    fun getAllSessions(): List<FocusSession> {
        val result = mutableListOf<FocusSession>()
        readableDatabase.query(
            TABLE_SESSIONS, null, null, null, null, null, "$COL_START_TIME DESC"
        ).use { c ->
            while (c.moveToNext()) {
                result += FocusSession(
                    id = c.getLong(c.getColumnIndexOrThrow(COL_ID)),
                    subject = c.getString(c.getColumnIndexOrThrow(COL_SUBJECT)),
                    startTime = c.getLong(c.getColumnIndexOrThrow(COL_START_TIME)),
                    durationSeconds = c.getLong(c.getColumnIndexOrThrow(COL_DURATION)),
                    distractions = c.getInt(c.getColumnIndexOrThrow(COL_DISTRACTIONS)),
                    avgLux = c.getFloat(c.getColumnIndexOrThrow(COL_AVG_LUX)),
                    note = c.getString(c.getColumnIndexOrThrow(COL_NOTE))
                )
            }
        }
        return result
    }

    // ---------- UPDATE ----------
    fun updateSession(session: FocusSession): Int =
        writableDatabase.update(
            TABLE_SESSIONS,
            session.toContentValues(),
            "$COL_ID = ?",
            arrayOf(session.id.toString())
        )

    // ---------- DELETE ----------
    fun deleteSession(id: Long): Int =
        writableDatabase.delete(TABLE_SESSIONS, "$COL_ID = ?", arrayOf(id.toString()))

    fun deleteAllSessions(): Int = writableDatabase.delete(TABLE_SESSIONS, null, null)

    private fun FocusSession.toContentValues() = ContentValues().apply {
        put(COL_SUBJECT, subject)
        put(COL_START_TIME, startTime)
        put(COL_DURATION, durationSeconds)
        put(COL_DISTRACTIONS, distractions)
        put(COL_AVG_LUX, avgLux)
        put(COL_NOTE, note)
    }
}
