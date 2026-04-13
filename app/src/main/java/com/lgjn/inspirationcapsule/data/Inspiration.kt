package com.lgjn.inspirationcapsule.data

data class Inspiration(
    val id: Long = 0,
    var title: String = "",
    val content: String = "",
    val audioPath: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = STATUS_ACTIVE,
    val transcript: String? = null
) {
    fun formattedDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(createdAt))
    }

    companion object {
        const val STATUS_ACTIVE    = "active"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_DISCARDED = "discarded"
    }
}
