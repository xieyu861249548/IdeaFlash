package com.lgjn.inspirationcapsule.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InspirationRepository(context: Context) {

    private val dbHelper = InspirationDbHelper(context)

    suspend fun getAll(): List<Inspiration> = withContext(Dispatchers.IO) {
        dbHelper.getAllInspirations()
    }

    suspend fun getActive(): List<Inspiration> = withContext(Dispatchers.IO) {
        dbHelper.getActiveInspirations()
    }

    suspend fun getByStatus(status: String): List<Inspiration> = withContext(Dispatchers.IO) {
        dbHelper.getInspirationsByStatus(status)
    }

    suspend fun insert(inspiration: Inspiration): Long = withContext(Dispatchers.IO) {
        dbHelper.insertInspiration(inspiration)
    }

    suspend fun updateContent(id: Long, content: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateContent(id, content)
    }

    suspend fun updateStatus(id: Long, status: String): Boolean = withContext(Dispatchers.IO) {
        dbHelper.updateStatus(id, status)
    }

    suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) {
        dbHelper.deleteInspiration(id)
    }
}
