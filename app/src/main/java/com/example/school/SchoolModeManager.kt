package com.example.school

import com.example.data.model.Task
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SchoolConflictResolutionResult(
    val organizedTasks: List<Task>,
    val notificationMessage: String? = null
)

object SchoolModeManager {

    val SCHOOL_SUBJECTS = listOf(
        "Matemáticas",
        "Lengua y Literatura",
        "Ciencias / Biología",
        "Física y Química",
        "Historia / Geografía",
        "Inglés",
        "Tecnología / Informática",
        "Arte / Música",
        "Educación Física",
        "Filosofía",
        "Otro"
    )

    val SCHOOL_TASK_TYPES = listOf(
        "deber" to "Deberes / Tarea",
        "trabajo" to "Trabajo / Proyecto",
        "examen" to "Estudio para Examen",
        "actividad" to "Actividad / Práctica",
        "pendiente" to "Pendiente escolar"
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parseTime(timeStr: String, fallbackHour: Int = 12): LocalTime {
        if (timeStr.isBlank()) return LocalTime.of(fallbackHour.coerceIn(0, 23), 0)
        return try {
            val trimmed = timeStr.trim().uppercase()
            val isPm = trimmed.contains("PM") || trimmed.contains("P.M.")
            val isAm = trimmed.contains("AM") || trimmed.contains("A.M.")
            val clean = trimmed.replace("PM", "").replace("AM", "").replace("P.M.", "").replace("A.M.", "").trim()
            val parts = clean.split(":")
            var hour = parts[0].trim().toIntOrNull() ?: fallbackHour
            val minute = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        } catch (e: Exception) {
            LocalTime.of(fallbackHour.coerceIn(0, 23), 0)
        }
    }

    fun formatTime(time: LocalTime): String = time.format(timeFormatter)

    fun isOverlapping(startA: LocalTime, endA: LocalTime, startB: LocalTime, endB: LocalTime): Boolean {
        return startA.isBefore(endB) && endA.isAfter(startB)
    }

    /**
     * Intelligently organizes school tasks:
     * - Detects overlapping time conflicts between tasks.
     * - If both tasks allow simultaneous execution (allowSimultaneous == true), they are preserved at the same time.
     * - If they cannot be simultaneous:
     *     - Compares priority: Urgent (4) > High (3) > Medium (2) > Low (1).
     *     - The higher priority task retains its desired schedule.
     *     - The lower priority task is automatically rescheduled to immediately after the higher priority one.
     * - Preserves user schedule if no conflict exists.
     */
    fun organizeSchoolTasks(
        allTasks: List<Task>,
        taskToUpsert: Task? = null
    ): SchoolConflictResolutionResult {
        // Build working list with taskToUpsert included or updated
        val workingList = allTasks.toMutableList()
        if (taskToUpsert != null) {
            val idx = workingList.indexOfFirst { it.id == taskToUpsert.id }
            if (idx >= 0) {
                workingList[idx] = taskToUpsert
            } else {
                workingList.add(taskToUpsert)
            }
        }

        // Only process school tasks
        val schoolTasks = workingList.filter { it.isSchoolTask || it.category.equals("Colegio", ignoreCase = true) }
        if (schoolTasks.size <= 1) {
            return SchoolConflictResolutionResult(organizedTasks = workingList)
        }

        val updatedMap = mutableMapOf<String, Task>()
        var conflictMessage: String? = null

        // Sort by start time, then priority descending
        val sorted = schoolTasks.sortedWith(
            compareBy<Task> {
                val t = it.scheduledStartTime.ifBlank { it.dueTime }.ifBlank { "23:59" }
                t
            }.thenByDescending { it.priorityWeight }
        )

        val scheduledItems = mutableListOf<Task>()

        for (current in sorted) {
            val currentStartTimeStr = current.scheduledStartTime.ifBlank { current.dueTime }.ifBlank { "08:00" }
            var currentStart = parseTime(currentStartTimeStr, 8)
            val duration = current.estimatedMinutes.coerceIn(15, 240)
            var currentEnd = currentStart.plusMinutes(duration.toLong())

            var resolvedTask = current.copy(
                scheduledStartTime = formatTime(currentStart),
                scheduledEndTime = formatTime(currentEnd)
            )

            // Check conflicts against already scheduled items
            var conflictFound = false
            for (prior in scheduledItems) {
                val priorStart = parseTime(prior.scheduledStartTime, 8)
                val priorEnd = parseTime(prior.scheduledEndTime, 9)

                // If both allow simultaneous, allow them to run concurrently
                if (prior.allowSimultaneous && resolvedTask.allowSimultaneous) {
                    continue
                }

                // If overlapping in time
                if (isOverlapping(currentStart, currentEnd, priorStart, priorEnd)) {
                    conflictFound = true
                    // Higher priority keeps slot; lower priority shifts after
                    if (resolvedTask.priorityWeight > prior.priorityWeight) {
                        // Current is more important: shift prior after current
                        val newPriorStart = currentEnd
                        val priorDuration = prior.estimatedMinutes.coerceIn(15, 240)
                        val newPriorEnd = newPriorStart.plusMinutes(priorDuration.toLong())
                        val updatedPrior = prior.copy(
                            scheduledStartTime = formatTime(newPriorStart),
                            scheduledEndTime = formatTime(newPriorEnd)
                        )
                        updatedMap[prior.id] = updatedPrior
                        conflictMessage = "Horario reorganizado: '${prior.title}' se movió a las ${formatTime(newPriorStart)} para priorizar '${resolvedTask.title}'."
                    } else {
                        // Prior is more or equal priority: shift current after prior
                        currentStart = priorEnd
                        currentEnd = currentStart.plusMinutes(duration.toLong())
                        resolvedTask = resolvedTask.copy(
                            scheduledStartTime = formatTime(currentStart),
                            scheduledEndTime = formatTime(currentEnd)
                        )
                        conflictMessage = "Conflicto resuelto: '${resolvedTask.title}' se programó a las ${formatTime(currentStart)} tras '${prior.title}'."
                    }
                }
            }

            scheduledItems.add(resolvedTask)
            updatedMap[resolvedTask.id] = resolvedTask
        }

        // Merge updated school tasks back into workingList
        val finalResult = workingList.map { original ->
            updatedMap[original.id] ?: original
        }

        return SchoolConflictResolutionResult(
            organizedTasks = finalResult,
            notificationMessage = conflictMessage
        )
    }

    /**
     * Handles "Necesito más tiempo":
     * Extends duration by additionalMinutes and shifts any colliding subsequent tasks forward.
     */
    fun extendTaskDuration(
        targetTask: Task,
        additionalMinutes: Int,
        allTasks: List<Task>
    ): SchoolConflictResolutionResult {
        val oldDuration = targetTask.estimatedMinutes
        val newDuration = oldDuration + additionalMinutes

        val start = parseTime(targetTask.scheduledStartTime.ifBlank { targetTask.dueTime }.ifBlank { "08:00" })
        val newEnd = start.plusMinutes(newDuration.toLong())

        val updatedTarget = targetTask.copy(
            estimatedMinutes = newDuration,
            scheduledStartTime = formatTime(start),
            scheduledEndTime = formatTime(newEnd),
            postScheduleOutcome = "need_more_time"
        )

        val result = organizeSchoolTasks(allTasks, updatedTarget)
        val msg = "Duración ampliada +$additionalMinutes min para '${targetTask.title}'. Horario de clases ajustado."
        return result.copy(notificationMessage = result.notificationMessage ?: msg)
    }
}
