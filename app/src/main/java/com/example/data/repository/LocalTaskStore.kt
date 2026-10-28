package com.example.data.repository

import android.content.Context
import com.example.data.model.SubTask
import com.example.data.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalTaskStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("planflow_local_tasks", Context.MODE_PRIVATE)
    private val keyTasks = "key_stored_tasks_json"

    private val _tasksFlow = MutableStateFlow<List<Task>>(loadTasksFromDisk())
    val tasksFlow: StateFlow<List<Task>> = _tasksFlow.asStateFlow()

    fun getTasks(): List<Task> = _tasksFlow.value

    @Synchronized
    private fun loadTasksFromDisk(): List<Task> {
        val jsonStr = prefs.getString(keyTasks, null) ?: return emptyList()
        val list = mutableListOf<Task>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val subtasksList = mutableListOf<SubTask>()
                if (obj.has("subtasks")) {
                    val subArr = obj.getJSONArray("subtasks")
                    for (j in 0 until subArr.length()) {
                        val subObj = subArr.getJSONObject(j)
                        subtasksList.add(
                            SubTask(
                                id = subObj.optString("id", ""),
                                title = subObj.optString("title", ""),
                                isCompleted = subObj.optBoolean("isCompleted", false)
                            )
                        )
                    }
                }
                list.add(
                    Task(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", "local_user"),
                        title = obj.optString("title", ""),
                        description = obj.optString("description", ""),
                        dueDate = obj.optString("dueDate", ""),
                        dueTime = obj.optString("dueTime", ""),
                        priority = obj.optString("priority", "medium"),
                        category = obj.optString("category", "General"),
                        estimatedMinutes = obj.optInt("estimatedMinutes", 30),
                        deadline = obj.optString("deadline", ""),
                        status = obj.optString("status", "pending"),
                        completedAt = obj.optString("completedAt", ""),
                        reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 15),
                        isRecurring = obj.optBoolean("isRecurring", false),
                        recurrenceRule = obj.optString("recurrenceRule", "none"),
                        subtasks = subtasksList,
                        projectId = obj.optString("projectId", ""),
                        scheduledStartTime = obj.optString("scheduledStartTime", ""),
                        scheduledEndTime = obj.optString("scheduledEndTime", ""),
                        scheduledDate = obj.optString("scheduledDate", ""),
                        orderIndex = obj.optInt("orderIndex", 0),
                        lastModifiedDevice = obj.optString("lastModifiedDevice", "Android Phone"),
                        isSchoolTask = obj.optBoolean("isSchoolTask", false),
                        allowSimultaneous = obj.optBoolean("allowSimultaneous", false),
                        schoolSubject = obj.optString("schoolSubject", ""),
                        schoolTaskType = obj.optString("schoolTaskType", "deber"),
                        postScheduleOutcome = obj.optString("postScheduleOutcome", "")
                    )
                )
            }
        } catch (e: Exception) {
            // Return empty list on parse error
        }
        return list.sortedWith(
            compareByDescending<Task> { it.priorityWeight }
                .thenBy { it.dueDate }
                .thenBy { it.dueTime }
        )
    }

    @Synchronized
    fun saveTasksToDisk(tasks: List<Task>) {
        val jsonArray = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("userId", task.userId)
                put("title", task.title)
                put("description", task.description)
                put("dueDate", task.dueDate)
                put("dueTime", task.dueTime)
                put("priority", task.priority)
                put("category", task.category)
                put("estimatedMinutes", task.estimatedMinutes)
                put("deadline", task.deadline)
                put("status", task.status)
                put("completedAt", task.completedAt)
                put("reminderMinutesBefore", task.reminderMinutesBefore)
                put("isRecurring", task.isRecurring)
                put("recurrenceRule", task.recurrenceRule)
                put("projectId", task.projectId)
                put("scheduledStartTime", task.scheduledStartTime)
                put("scheduledEndTime", task.scheduledEndTime)
                put("scheduledDate", task.scheduledDate)
                put("orderIndex", task.orderIndex)
                put("lastModifiedDevice", task.lastModifiedDevice)
                put("isSchoolTask", task.isSchoolTask)
                put("allowSimultaneous", task.allowSimultaneous)
                put("schoolSubject", task.schoolSubject)
                put("schoolTaskType", task.schoolTaskType)
                put("postScheduleOutcome", task.postScheduleOutcome)

                val subArr = JSONArray()
                for (st in task.subtasks) {
                    val subObj = JSONObject().apply {
                        put("id", st.id)
                        put("title", st.title)
                        put("isCompleted", st.isCompleted)
                    }
                    subArr.put(subObj)
                }
                put("subtasks", subArr)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(keyTasks, jsonArray.toString()).apply()
        _tasksFlow.value = tasks.sortedWith(
            compareByDescending<Task> { it.priorityWeight }
                .thenBy { it.dueDate }
                .thenBy { it.dueTime }
        )
    }

    @Synchronized
    fun addOrUpdateTask(task: Task): String {
        val current = _tasksFlow.value.toMutableList()
        val taskId = if (task.id.isBlank()) UUID.randomUUID().toString() else task.id
        val finalizedTask = task.copy(id = taskId)

        val index = current.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            current[index] = finalizedTask
        } else {
            current.add(finalizedTask)
        }
        saveTasksToDisk(current)
        return taskId
    }

    @Synchronized
    fun deleteTask(taskId: String) {
        val current = _tasksFlow.value.toMutableList()
        current.removeAll { it.id == taskId }
        saveTasksToDisk(current)
    }

    @Synchronized
    fun clearTasks() {
        prefs.edit().remove(keyTasks).apply()
        _tasksFlow.value = emptyList()
    }
}
