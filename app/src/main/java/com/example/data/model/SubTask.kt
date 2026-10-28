package com.example.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class SubTask(
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false
)
