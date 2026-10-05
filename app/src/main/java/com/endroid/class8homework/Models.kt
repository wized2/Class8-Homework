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
    data class Info(val key: String, val label: String, val iconRes: Int)

    val ALL = listOf(
        Info("english", "English", R.drawable.ic_subj_english),
        Info("urdu", "Urdu", R.drawable.ic_subj_urdu),
        Info("mathematics", "Mathematics", R.drawable.ic_subj_math),
        Info("science", "Science", R.drawable.ic_subj_science),
        Info("islamiat", "Islamiat", R.drawable.ic_subj_islamiat),
        Info("tarjama-tul-quran", "Tarjama-tul-Quran", R.drawable.ic_subj_quran),
        Info("ethics", "Ethics", R.drawable.ic_subj_ethics),
        Info("drawing", "Drawing", R.drawable.ic_subj_drawing),
        Info("geography", "Geography", R.drawable.ic_subj_geo),
        Info("history", "History", R.drawable.ic_subj_history),
        Info("computer-science", "Computer Science", R.drawable.ic_subj_cs),
    )
}
