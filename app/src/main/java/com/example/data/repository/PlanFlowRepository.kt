package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Goal
import com.example.data.model.Project
import com.example.data.model.ScheduleBlock
import com.example.data.model.SubTask
import com.example.data.model.SyncEvent
import com.example.data.model.Task
import com.example.data.model.UserPreferences
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class PlanFlowRepository(
    private val db: FirebaseFirestore,
    private val context: Context? = null
) {

    private val localStore: LocalTaskStore? = context?.let { LocalTaskStore(it) }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        context.applicationContext
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // ==========================================
    // 1. TASKS
    // ==========================================

    fun observeTasks(userId: String): Flow<List<Task>> = flow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null || userId == "guest_local") {
            localStore?.let { store ->
                emitAll(store.tasksFlow)
            } ?: emit(emptyList())
            return@flow
        }

        // First emit cached tasks so UI displays instantly and is available offline
        localStore?.getTasks()?.takeIf { it.isNotEmpty() }?.let { cached ->
            emit(cached)
        }

        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    val list = snapshot.toObjects(Task::class.java).sortedWith(
                        compareByDescending<Task> { it.priorityWeight }
                            .thenBy { it.dueDate }
                            .thenBy { it.dueTime }
                    )
                    localStore?.saveTasksToDisk(list)
                    list
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    localStore?.getTasks()?.let { emit(it) }
                }
        )
    }

    suspend fun createTask(task: Task, sourceDevice: String = "Android Phone"): String {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            val localId = localStore?.addOrUpdateTask(task.copy(userId = "guest_local")) ?: UUID.randomUUID().toString()
            return localId
        }

        val docRef = db.collection("users").document(uid).collection("tasks").document()
        val taskId = docRef.id

        val taskData = mapOf(
            "userId" to uid,
            "title" to task.title,
            "description" to task.description,
            "dueDate" to task.dueDate,
            "dueTime" to task.dueTime,
            "priority" to task.priority,
            "category" to task.category,
            "estimatedMinutes" to task.estimatedMinutes,
            "deadline" to task.deadline,
            "status" to task.status,
            "completedAt" to task.completedAt,
            "reminderMinutesBefore" to task.reminderMinutesBefore,
            "isRecurring" to task.isRecurring,
            "recurrenceRule" to task.recurrenceRule,
            "subtasks" to task.subtasks.map { mapOf("id" to it.id, "title" to it.title, "isCompleted" to it.isCompleted) },
            "projectId" to task.projectId,
            "scheduledStartTime" to task.scheduledStartTime,
            "scheduledEndTime" to task.scheduledEndTime,
            "scheduledDate" to task.scheduledDate,
            "orderIndex" to task.orderIndex,
            "lastModifiedDevice" to sourceDevice,
            "isSchoolTask" to task.isSchoolTask,
            "allowSimultaneous" to task.allowSimultaneous,
            "schoolSubject" to task.schoolSubject,
            "schoolTaskType" to task.schoolTaskType,
            "postScheduleOutcome" to task.postScheduleOutcome,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.set(taskData).await()
            localStore?.addOrUpdateTask(task.copy(id = taskId, userId = uid))
            // Trigger cross-device sync event
            broadcastSyncEvent(
                SyncEvent(
                    userId = uid,
                    sourceDevice = sourceDevice,
                    targetDevice = "all",
                    eventType = "task_created",
                    taskTitle = task.title,
                    message = "Nueva tarea creada: ${task.title}"
                )
            )
            return taskId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            localStore?.addOrUpdateTask(task.copy(id = taskId, userId = uid))
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            return taskId
        }
    }

    suspend fun updateTask(task: Task, sourceDevice: String = "Android Phone") {
        localStore?.addOrUpdateTask(task)
        val uid = auth.currentUser?.uid ?: return
        if (task.id.isBlank()) return
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)

        val updates = mutableMapOf<String, Any>(
            "userId" to uid,
            "title" to task.title,
            "description" to task.description,
            "dueDate" to task.dueDate,
            "dueTime" to task.dueTime,
            "priority" to task.priority,
            "category" to task.category,
            "estimatedMinutes" to task.estimatedMinutes,
            "deadline" to task.deadline,
            "status" to task.status,
            "completedAt" to task.completedAt,
            "reminderMinutesBefore" to task.reminderMinutesBefore,
            "isRecurring" to task.isRecurring,
            "recurrenceRule" to task.recurrenceRule,
            "subtasks" to task.subtasks.map { mapOf("id" to it.id, "title" to it.title, "isCompleted" to it.isCompleted) },
            "projectId" to task.projectId,
            "scheduledStartTime" to task.scheduledStartTime,
            "scheduledEndTime" to task.scheduledEndTime,
            "scheduledDate" to task.scheduledDate,
            "orderIndex" to task.orderIndex,
            "lastModifiedDevice" to sourceDevice,
            "isSchoolTask" to task.isSchoolTask,
            "allowSimultaneous" to task.allowSimultaneous,
            "schoolSubject" to task.schoolSubject,
            "schoolTaskType" to task.schoolTaskType,
            "postScheduleOutcome" to task.postScheduleOutcome,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        }
    }

    suspend fun toggleTaskCompletion(task: Task, sourceDevice: String = "Android Phone") {
        val isNowCompleted = task.status != "completed"
        val newStatus = if (isNowCompleted) "completed" else "pending"
        val completedAt = if (isNowCompleted) LocalDate.now().toString() else ""

        val updatedTask = task.copy(
            status = newStatus,
            completedAt = completedAt,
            lastModifiedDevice = sourceDevice
        )
        updateTask(updatedTask, sourceDevice)

        if (isNowCompleted) {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                broadcastSyncEvent(
                    SyncEvent(
                        userId = uid,
                        sourceDevice = sourceDevice,
                        targetDevice = "all",
                        eventType = "task_completed",
                        taskTitle = task.title,
                        message = "Tarea completada en $sourceDevice: ${task.title}"
                    )
                )
            }
        }
    }

    suspend fun toggleSubtask(task: Task, subtaskId: String, sourceDevice: String = "Android Phone") {
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        val allCompleted = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isCompleted }
        val newStatus = if (allCompleted) "completed" else if (updatedSubtasks.any { it.isCompleted }) "in_progress" else task.status

        val updatedTask = task.copy(
            subtasks = updatedSubtasks,
            status = newStatus,
            lastModifiedDevice = sourceDevice
        )
        updateTask(updatedTask, sourceDevice)
    }

    suspend fun deleteTask(taskId: String) {
        localStore?.deleteTask(taskId)
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
        }
    }

    // ==========================================
    // 2. AUTOMATIC DAY PLANNING (RULES-BASED, NO AI)
    // ==========================================

    /**
     * Organizes today's tasks using a deterministic rule-based scheduling algorithm:
     * 1. Prioritize tasks: Urgent (weight 4) > High (3) > Medium (2) > Low (1)
     * 2. Incorporate explicit due dates and deadlines
     * 3. Allocate tasks according to their estimated durations
     * 4. Prevent overlapping time slots
     * 5. Fit tasks into user's available working hours (e.g. 08:00 - 18:00)
     * 6. Insert rest/break buffers automatically between intense blocks
     * 7. Save scheduled slots to Firestore so all devices stay synchronized
     */
    suspend fun autoPlanDay(
        tasks: List<Task>,
        preferences: UserPreferences,
        targetDate: LocalDate = LocalDate.now(),
        sourceDevice: String = "Android Phone"
    ): List<ScheduleBlock> {
        val scheduleBlocks = mutableListOf<ScheduleBlock>()
        try {
            val targetDateStr = targetDate.toString()
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

            // 1. Filter actionable tasks: pending or in_progress, for today or without date
            val candidateTasks = tasks.filter {
                it.status != "completed" && (it.dueDate.isBlank() || it.dueDate <= targetDateStr)
            }

            if (candidateTasks.isEmpty()) {
                return emptyList()
            }

            // Separate tasks into:
            // A. Fixed tasks: user explicitly specified a dueTime (e.g. "15:00")
            // B. Flexible tasks: no specific time, placed into available gaps by priority
            val fixedTasks = candidateTasks.filter { it.dueTime.isNotBlank() }
            val flexibleTasks = candidateTasks.filter { it.dueTime.isBlank() }
                .sortedWith(
                    compareByDescending<Task> { it.priorityWeight }
                        .thenByDescending { it.isOverdue }
                        .thenBy { it.estimatedMinutes }
                )

            val workStartHour = preferences.workStartHour.coerceIn(0, 22)
            val workEndHour = preferences.workEndHour.coerceIn(workStartHour + 1, 23)
            val breakMinutes = preferences.breakDurationMinutes.coerceIn(5, 30)

            // Step 1: Schedule fixed tasks at their user-selected dueTime
            for (task in fixedTasks) {
                val parsedStartTime = parseTimeString(task.dueTime, workStartHour)
                val duration = task.estimatedMinutes.coerceIn(15, 180)
                val parsedEndTime = parsedStartTime.plusMinutes(duration.toLong())

                val startStr = parsedStartTime.format(timeFormatter)
                val endStr = parsedEndTime.format(timeFormatter)

                scheduleBlocks.add(
                    ScheduleBlock(
                        task = task,
                        startTime = startStr,
                        endTime = endStr,
                        durationMinutes = duration,
                        isBreak = false
                    )
                )

                // Update scheduled slot without changing the task's dueTime!
                val scheduledTask = task.copy(
                    scheduledStartTime = startStr,
                    scheduledEndTime = endStr,
                    scheduledDate = targetDateStr,
                    lastModifiedDevice = sourceDevice
                )
                try {
                    updateTask(scheduledTask, sourceDevice)
                } catch (e: Exception) {
                    // Safe guard
                }
            }

            // Step 2: Fit flexible tasks into available gaps during working hours
            var currentHour = workStartHour
            var currentMinute = 0
            var accumulatedWorkMinutes = 0

            for (task in flexibleTasks) {
                if (currentHour >= workEndHour) break

                val duration = task.estimatedMinutes.coerceIn(15, 120)
                var slotStart = LocalTime.of(currentHour.coerceIn(0, 23), currentMinute.coerceIn(0, 59))
                var slotEnd = slotStart.plusMinutes(duration.toLong())

                // Avoid collision with already scheduled fixed blocks
                var shiftAttempts = 0
                while (shiftAttempts < 8) {
                    shiftAttempts++
                    val overlapping = scheduleBlocks.find { block ->
                        !block.isBreak && isTimeOverlap(slotStart, slotEnd, block.startTime, block.endTime)
                    }
                    if (overlapping != null) {
                        val overEnd = parseTimeString(overlapping.endTime, currentHour)
                        if (overEnd.isAfter(slotStart) && overEnd.hour < workEndHour) {
                            slotStart = overEnd
                            slotEnd = slotStart.plusMinutes(duration.toLong())
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }

                if (slotEnd.hour > workEndHour || (slotEnd.hour == workEndHour && slotEnd.minute > 0)) {
                    break
                }

                val startStr = slotStart.format(timeFormatter)
                val endStr = slotEnd.format(timeFormatter)

                scheduleBlocks.add(
                    ScheduleBlock(
                        task = task,
                        startTime = startStr,
                        endTime = endStr,
                        durationMinutes = duration,
                        isBreak = false
                    )
                )

                val scheduledTask = task.copy(
                    scheduledStartTime = startStr,
                    scheduledEndTime = endStr,
                    scheduledDate = targetDateStr,
                    lastModifiedDevice = sourceDevice
                )
                try {
                    updateTask(scheduledTask, sourceDevice)
                } catch (e: Exception) {
                    // Safe guard
                }

                currentHour = slotEnd.hour
                currentMinute = slotEnd.minute
                accumulatedWorkMinutes += duration

                // Insert a break after 90+ minutes of continuous work
                if (accumulatedWorkMinutes >= 90 && currentHour < workEndHour) {
                    val breakStart = LocalTime.of(currentHour.coerceIn(0, 23), currentMinute.coerceIn(0, 59))
                    val breakEnd = breakStart.plusMinutes(breakMinutes.toLong())
                    if (breakEnd.hour <= workEndHour) {
                        scheduleBlocks.add(
                            ScheduleBlock(
                                task = Task(title = "Pausa activa / Descanso mental", category = "Salud"),
                                startTime = breakStart.format(timeFormatter),
                                endTime = breakEnd.format(timeFormatter),
                                durationMinutes = breakMinutes,
                                isBreak = true
                            )
                        )
                        currentHour = breakEnd.hour
                        currentMinute = breakEnd.minute
                        accumulatedWorkMinutes = 0
                    }
                }
            }

            // Sort all blocks chronologically by start time
            scheduleBlocks.sortBy { it.startTime }

            // Safe sync broadcast if signed in
            val currentUid = auth.currentUser?.uid
            if (currentUid != null) {
                try {
                    broadcastSyncEvent(
                        SyncEvent(
                            userId = currentUid,
                            sourceDevice = sourceDevice,
                            targetDevice = "all",
                            eventType = "schedule_updated",
                            taskTitle = "Agenda del día",
                            message = "Planificación automática del día generada con ${scheduleBlocks.filter { !it.isBreak }.size} tareas."
                        )
                    )
                } catch (e: Exception) {
                    // Safe
                }
            }
        } catch (e: Exception) {
            // Safe guard against any unexpected runtime failures
        }

        return scheduleBlocks
    }

    private fun isTimeOverlap(start1: LocalTime, end1: LocalTime, start2Str: String, end2Str: String): Boolean {
        return try {
            val start2 = parseTimeString(start2Str)
            val end2 = parseTimeString(end2Str)
            start1.isBefore(end2) && end1.isAfter(start2)
        } catch (e: Exception) {
            false
        }
    }

    fun parseTimeString(timeStr: String, defaultHour: Int = 12): LocalTime {
        if (timeStr.isBlank()) return LocalTime.of(defaultHour.coerceIn(0, 23), 0)
        return try {
            val trimmed = timeStr.trim().uppercase()
            val isPm = trimmed.contains("PM") || trimmed.contains("P.M.")
            val isAm = trimmed.contains("AM") || trimmed.contains("A.M.")
            val clean = trimmed.replace("PM", "").replace("AM", "").replace("P.M.", "").replace("A.M.", "").trim()
            val parts = clean.split(":")
            var hour = parts[0].trim().toIntOrNull() ?: defaultHour
            val minute = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0
            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0
            LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        } catch (e: Exception) {
            LocalTime.of(defaultHour.coerceIn(0, 23), 0)
        }
    }

    // ==========================================
    // 3. PROJECTS & GOALS
    // ==========================================

    fun observeProjects(userId: String): Flow<List<Project>> = flow {
        val path = "users/$userId/projects"
        emitAll(
            db.collection("users").document(userId).collection("projects")
                .snapshots()
                .map { it.toObjects(Project::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createProject(project: Project) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document()
        val data = mapOf(
            "userId" to uid,
            "name" to project.name,
            "colorHex" to project.colorHex,
            "iconName" to project.iconName,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeGoals(userId: String): Flow<List<Goal>> = flow {
        val path = "users/$userId/goals"
        emitAll(
            db.collection("users").document(userId).collection("goals")
                .snapshots()
                .map { it.toObjects(Goal::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createGoal(goal: Goal) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document()
        val data = mapOf(
            "userId" to uid,
            "title" to goal.title,
            "targetCount" to goal.targetCount,
            "currentCount" to goal.currentCount,
            "unit" to goal.unit,
            "deadline" to goal.deadline,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateGoalProgress(goalId: String, increment: Int) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("goals").document(goalId)
        try {
            docRef.update("currentCount", FieldValue.increment(increment.toLong())).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // ==========================================
    // 4. USER PREFERENCES & CROSS-DEVICE SYNC
    // ==========================================

    fun observePreferences(userId: String): Flow<UserPreferences> = flow {
        val path = "users/$userId/preferences/default"
        emitAll(
            db.collection("users").document(userId).collection("preferences").document("default")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(UserPreferences::class.java) ?: UserPreferences(userId = userId)
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun savePreferences(preferences: UserPreferences) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("preferences").document("default")
        val data = mapOf(
            "userId" to uid,
            "workStartHour" to preferences.workStartHour,
            "workEndHour" to preferences.workEndHour,
            "breakDurationMinutes" to preferences.breakDurationMinutes,
            "quietHoursStart" to preferences.quietHoursStart,
            "quietHoursEnd" to preferences.quietHoursEnd,
            "doNotDisturb" to preferences.doNotDisturb,
            "notificationsEnabled" to preferences.notificationsEnabled,
            "crossDeviceNotifications" to preferences.crossDeviceNotifications,
            "activeDeviceId" to preferences.activeDeviceId,
            "deviceName" to preferences.deviceName,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeSyncEvents(userId: String): Flow<List<SyncEvent>> = flow {
        val path = "users/$userId/sync_events"
        emitAll(
            db.collection("users").document(userId).collection("sync_events")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SyncEvent::class.java).sortedByDescending { it.timestamp?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun broadcastSyncEvent(event: SyncEvent) {
        val uid = event.userId.ifBlank { auth.currentUser?.uid ?: return }
        val docRef = db.collection("users").document(uid).collection("sync_events").document()
        val data = mapOf(
            "userId" to uid,
            "sourceDevice" to event.sourceDevice,
            "targetDevice" to event.targetDevice,
            "eventType" to event.eventType,
            "taskTitle" to event.taskTitle,
            "message" to event.message,
            "timestamp" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(data).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }

    // Wipe all user data to reset the database completely clean like brand new
    suspend fun clearAllData(userId: String) {
        localStore?.clearTasks()
        val userDoc = db.collection("users").document(userId)
        val collections = listOf("tasks", "projects", "goals", "sync_events")
        for (col in collections) {
            try {
                val snapshot = userDoc.collection(col).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                // Ignore collection delete error
            }
        }
    }

    suspend fun saveBatchTasks(tasks: List<Task>, sourceDevice: String = "Android Phone") {
        for (task in tasks) {
            updateTask(task, sourceDevice)
        }
    }
}
