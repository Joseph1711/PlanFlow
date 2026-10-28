package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Goal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetCount: Int = 5,
    val currentCount: Int = 0,
    val unit: String = "tareas",
    val deadline: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val progressFraction: Float
        get() = if (targetCount > 0) (currentCount.toFloat() / targetCount).coerceIn(0f, 1f) else 0f
}
