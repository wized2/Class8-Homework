package com.endroid.class8homework

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for https://ai.endroid.workers.dev/
 *
 * POST JSON: { "prompt": String, "history": [{role, content}], "web_search": Boolean }
 * Response: text/plain (Markdown)
 */
class AiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val ENDPOINT = "https://ai.endroid.workers.dev/"

        /** Injected as conversation priming so answers fit Class 8 (worker has its own system prompt). */
        val CLASS8_PRIMER = listOf(
            ChatMessage(
                role = "user",
                content = """
You are now in **Class 8 Study Helper** mode for this chat.
Rules:
- Teach like a kind, patient Class 8 teacher (age ~13–14).
- Prefer **simple English** and/or **Urdu (اردو)** — match the student's language; mix both when it helps.
- Use **Markdown** (headings, bold, lists, steps). Never raw HTML.
- Explain step-by-step; give short examples from a Class 8 level (math, science, English, Urdu, Islamiat, etc.).
- Encourage the student; never shame mistakes.
- If a question is above Class 8, still help gently but say it may be advanced.
Reply with one short confirmation that you are ready as Class 8 helper (Urdu + English is fine).
                """.trim()
            ),
            ChatMessage(
                role = "model",
                content = "Theek hai! Main aapka Class 8 study helper hoon. English ya Urdu mein poochhein — math, science, grammar, kuch bhi. Let's learn together!"
            )
        )
    }

    data class ChatMessage(val role: String, val content: String)

    suspend fun ask(
        prompt: String,
        history: List<ChatMessage>,
        webSearch: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val hist = JSONArray()
            for (m in history) {
                hist.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            val bodyJson = JSONObject()
                .put("prompt", prompt)
                .put("web_search", webSearch)
                .put("history", hist)
                .toString()

            val req = Request.Builder()
                .url(ENDPOINT)
                .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Accept", "text/plain")
                .build()

            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty().trim()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(
                        Exception(text.ifBlank { "HTTP ${resp.code}" })
                    )
                }
                if (text.isBlank()) {
                    return@withContext Result.failure(Exception("Empty response"))
                }
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
