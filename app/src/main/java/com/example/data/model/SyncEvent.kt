package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SyncEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sourceDevice: String = "PC Desktop",
    val targetDevice: String = "all",
    val eventType: String = "task_created", // task_created, task_completed, reminder_triggered, schedule_updated
    val taskTitle: String = "",
    val message: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
)
