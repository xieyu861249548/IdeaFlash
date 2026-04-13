package com.lgjn.inspirationcapsule.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class InspirationDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "inspirations.db"
        const val DATABASE_VERSION = 2
        const val TABLE_NAME = "inspirations"
        const val COL_ID = "_id"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "content"
        const val COL_AUDIO_PATH = "audio_path"
        const val COL_CREATED_AT = "created_at"
        const val COL_STATUS = "status"
        const val COL_TRANSCRIPT = "transcript"

        private const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL DEFAULT '',
                $COL_CONTENT TEXT NOT NULL,
                $COL_AUDIO_PATH TEXT NOT NULL DEFAULT '',
                $COL_CREATED_AT INTEGER NOT NULL,
                $COL_STATUS TEXT NOT NULL DEFAULT '${Inspiration.STATUS_ACTIVE}',
                $COL_TRANSCRIPT TEXT
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_STATUS TEXT NOT NULL DEFAULT '${Inspiration.STATUS_ACTIVE}'")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COL_TRANSCRIPT TEXT")
        }
    }

    fun insertInspiration(inspiration: Inspiration): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, inspiration.title)
            put(COL_CONTENT, inspiration.content)
            put(COL_AUDIO_PATH, inspiration.audioPath)
            put(COL_CREATED_AT, inspiration.createdAt)
            put(COL_STATUS, inspiration.status)
            put(COL_TRANSCRIPT, inspiration.transcript)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    fun updateContent(id: Long, content: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CONTENT, content)
        }
        val rows = db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun updateStatus(id: Long, status: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_STATUS, status)
        }
        val rows = db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun deleteInspiration(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    fun getAllInspirations(): List<Inspiration> = queryByStatus(null)

    fun getActiveInspirations(): List<Inspiration> = queryByStatus(Inspiration.STATUS_ACTIVE)

    fun getInspirationsByStatus(status: String): List<Inspiration> = queryByStatus(status)

    private fun queryByStatus(status: String?): List<Inspiration> {
        val list = mutableListOf<Inspiration>()
        val db = readableDatabase
        val selection = if (status != null) "$COL_STATUS = ?" else null
        val selectionArgs = if (status != null) arrayOf(status) else null
        val cursor = db.query(
            TABLE_NAME, null, selection, selectionArgs, null, null,
            "$COL_CREATED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Inspiration(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        content = it.getString(it.getColumnIndexOrThrow(COL_CONTENT)),
                        audioPath = it.getString(it.getColumnIndexOrThrow(COL_AUDIO_PATH)),
                        createdAt = it.getLong(it.getColumnIndexOrThrow(COL_CREATED_AT)),
                        status = it.getString(it.getColumnIndexOrThrow(COL_STATUS)),
                        transcript = it.getString(it.getColumnIndexOrThrow(COL_TRANSCRIPT))
                    )
                )
            }
        }
        return list
    }
}
