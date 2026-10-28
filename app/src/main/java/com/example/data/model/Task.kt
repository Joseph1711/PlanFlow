package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val dueDate: String = "",           // Format: YYYY-MM-DD
    val dueTime: String = "",           // Format: HH:mm
    val priority: String = "medium",     // urgent, high, medium, low
    val category: String = "General",    // Trabajo, Personal, Estudio, Salud, etc.
    val estimatedMinutes: Int = 30,
    val deadline: String = "",          // Specific deadline description or timestamp
    val status: String = "pending",     // pending, in_progress, completed
    val completedAt: String = "",
    val reminderMinutesBefore: Int = 15,
    val isRecurring: Boolean = false,
    val recurrenceRule: String = "none", // daily, weekly, monthly, none
    val subtasks: List<SubTask> = emptyList(),
    val projectId: String = "",
    val scheduledStartTime: String = "", // e.g. "09:00"
    val scheduledEndTime: String = "",   // e.g. "10:00"
    val scheduledDate: String = "",      // e.g. "2026-10-28"
    val orderIndex: Int = 0,
    val lastModifiedDevice: String = "Android Phone",
    val isSchoolTask: Boolean = false,
    val allowSimultaneous: Boolean = false,
    val schoolSubject: String = "",       // e.g. Matemáticas, Lengua, Ciencias, Historia, etc.
    val schoolTaskType: String = "deber", // deber, trabajo, examen, actividad, pendiente
    val postScheduleOutcome: String = "", // completed, incomplete, need_more_time
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val priorityWeight: Int
        get() = when (priority) {
            "urgent" -> 4
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 1
        }

    val isOverdue: Boolean
        get() {
            if (status == "completed" || dueDate.isBlank()) return false
            return try {
                val nowStr = java.time.LocalDate.now().toString()
                dueDate < nowStr
            } catch (e: Exception) {
                false
            }
        }
}
