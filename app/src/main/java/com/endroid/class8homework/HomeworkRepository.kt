package com.endroid.class8homework

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class HomeworkRepository(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("hw_cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    companion object {
        const val BASE = "https://class-8.pages.dev"
        const val READ = "$BASE/api/read"
    }

    fun loadCache(): HomeworkData? {
        val json = prefs.getString("last_success", null) ?: return null
        return parseData(JSONObject(json))
    }

    private fun saveCache(rawData: JSONObject) {
        prefs.edit().putString("last_success", rawData.toString()).apply()
    }

    suspend fun fetch(): HomeworkResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(READ).get().build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext HomeworkResult.ApiError("EMPTY_BODY", "Server returned no data.")
                }
                val root = JSONObject(body)
                val code = root.optString("code")
                val message = root.optString("message", "Something went wrong.")
                when (code) {
                    "SUCCESS" -> {
                        val dataObj = root.optJSONObject("data") ?: JSONObject()
                        saveCache(dataObj)
                        HomeworkResult.Loaded(parseData(dataObj), fromCache = false)
                    }
                    "NO_HOMEWORK" -> HomeworkResult.Empty(message)
                    "SERVER_ERROR" -> HomeworkResult.ApiError(code, message.ifBlank {
                        app.getString(R.string.service_unavailable)
                    })
                    else -> HomeworkResult.ApiError(code, message)
                }
            }
        } catch (_: Exception) {
            HomeworkResult.NetworkError(app.getString(R.string.network_error))
        }
    }

    private fun parseData(data: JSONObject): HomeworkData {
        val ts = if (data.isNull("timestamp")) null else data.optString("timestamp", null)
        val entriesObj = data.optJSONObject("entries") ?: JSONObject()
        val map = mutableMapOf<String, HomeworkEntry>()
        val keys = entriesObj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val o = entriesObj.optJSONObject(k) ?: continue
            map[k] = HomeworkEntry(
                subject = o.optString("subject", k),
                page = o.optString("page", ""),
                description = o.optString("description", ""),
                source = o.optString("source", "other"),
                notes = o.optString("notes", "")
            )
        }
        return HomeworkData(ts, map)
    }
}
