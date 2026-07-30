package com.eaglesistemas.eaglepbx.data

data class HistoryCall(
    val id: String,
    val direction: String,
    val remoteNumber: String,
    val remoteName: String,
    val remoteAvatar: String?,
    val startedAt: String,
    val durationSeconds: Int,
    val result: String,
    val recording: Boolean
) {
    val missed: Boolean
        get() = direction == "in" && (
            durationSeconds == 0 ||
                result.lowercase().contains(Regex(
                    "no answer|não atend|nao atend|missed|abandon|busy|ocupad"
                ))
            )
}
