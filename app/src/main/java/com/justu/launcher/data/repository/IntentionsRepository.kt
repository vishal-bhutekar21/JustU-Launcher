package com.justu.launcher.data.repository

import android.content.Context
import com.justu.launcher.data.model.DailyIntention
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentionsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val file: File get() = File(context.filesDir, "daily_intentions.json")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun todayKey(): String = dateFormat.format(Date())

    private fun readJson(): JSONObject {
        return if (file.exists()) {
            try { JSONObject(file.readText()) } catch (e: Exception) { JSONObject() }
        } else JSONObject()
    }

    private fun writeJson(json: JSONObject) {
        file.writeText(json.toString())
    }

    suspend fun saveIntention(text: String) = withContext(Dispatchers.IO) {
        val json = readJson()
        json.put(todayKey(), text)
        writeJson(json)
    }

    fun getTodayIntention(): Flow<String> = flow {
        val json = readJson()
        emit(json.optString(todayKey(), ""))
    }.flowOn(Dispatchers.IO)

    fun getAllIntentions(): Flow<List<DailyIntention>> = flow {
        val json = readJson()
        val list = mutableListOf<DailyIntention>()
        json.keys().forEach { key ->
            val text = json.optString(key, "")
            if (text.isNotEmpty()) {
                list.add(DailyIntention(date = key, text = text))
            }
        }
        // Sort by date descending (newest first)
        list.sortByDescending { it.date }
        emit(list)
    }.flowOn(Dispatchers.IO)
}
