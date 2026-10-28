package com.example.data.model

data class ScheduleBlock(
    val task: Task,
    val startTime: String,      // e.g. "09:00"
    val endTime: String,        // e.g. "10:00"
    val durationMinutes: Int,
    val isBreak: Boolean = false
)
