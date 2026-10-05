package com.endroid.class8homework

data class HomeworkEntry(
    val subject: String,
    val page: String,
    val description: String,
    val source: String,
    val notes: String = ""
)

data class HomeworkData(
    val timestamp: String?,
    val entries: Map<String, HomeworkEntry>
)

sealed class HomeworkResult {
    data class Loaded(val data: HomeworkData, val fromCache: Boolean = false) : HomeworkResult()
    data class Empty(val message: String) : HomeworkResult()
    data class ApiError(val code: String, val message: String) : HomeworkResult()
    data class NetworkError(val message: String) : HomeworkResult()
}

object Subjects {
    data class Info(val key: String, val label: String, val emoji: String)

    val ALL = listOf(
        Info("english", "English", "📘"),
        Info("urdu", "Urdu", "📗"),
        Info("mathematics", "Mathematics", "🔢"),
        Info("science", "Science", "🔬"),
        Info("islamiat", "Islamiat", "🌙"),
        Info("tarjama-tul-quran", "Tarjama-tul-Quran", "📖"),
        Info("ethics", "Ethics", "💜"),
        Info("drawing", "Drawing", "🎨"),
        Info("geography", "Geography", "🌍"),
        Info("history", "History", "🏺"),
        Info("computer-science", "Computer Science", "💻"),
    )
}
