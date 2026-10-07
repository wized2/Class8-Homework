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
 * POST: { prompt, history: [{role, content}] }
 * Response: text/plain Markdown
 */
class AiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val ENDPOINT = "https://ai.endroid.workers.dev/"

        fun class8Primer(studentName: String): List<ChatMessage> {
            val who = if (studentName.isBlank()) "the student" else studentName
            return listOf(
                ChatMessage(
                    role = "user",
                    content = """
You are in **Class 8 Study Helper** mode.
Student name: **${who}** — address them by name sometimes.

Rules:
- Kind, patient Class 8 teacher (ages ~13–14).
- Answer in **simple English** and/or **Urdu (اردو)**; match the student's language.
- Use **Markdown**. For math use LaTeX: inline ${'$'}a_9${'$'}, ${'$'}x^2${'$'}, ${'$'}\frac{a}{b}${'$'} and display ${'$'}${'$'}...${'$'}${'$'} for bigger formulas.
- Step-by-step explanations with Class 8 level examples.
- Encourage; never shame mistakes.
- If above Class 8, still help gently and note it may be advanced.
- When making MCQs, use this exact shape so the app can render them:
  1. Question text
  A) option
  B) option
  C) option
  D) option
  Answer: B
- For short Q&A drills use:
  Q: ...
  A: ...

Reply with one short confirmation that you are ready (can greet ${who}).
                    """.trim()
                ),
                ChatMessage(
                    role = "model",
                    content = if (studentName.isBlank()) {
                        "Theek hai! Main aapka Class 8 study helper hoon. English ya Urdu mein poochhein."
                    } else {
                        "Theek hai, $studentName! Main aapka Class 8 study helper hoon. English ya Urdu mein poochhein — math, science, grammar, kuch bhi."
                    }
                )
            )
        }
    }

    data class ChatMessage(val role: String, val content: String)

    suspend fun ask(
        prompt: String,
        history: List<ChatMessage>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val hist = JSONArray()
            for (m in history) {
                hist.put(JSONObject().put("role", m.role).put("content", m.content))
            }
            val bodyJson = JSONObject()
                .put("prompt", prompt)
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
                    return@withContext Result.failure(Exception(text.ifBlank { "HTTP ${resp.code}" }))
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
