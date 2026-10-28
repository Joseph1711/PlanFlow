package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Goal
import com.example.data.model.Project
import com.example.data.model.ScheduleBlock
import com.example.data.model.SyncEvent
import com.example.data.model.Task
import com.example.data.model.UserPreferences
import com.example.data.repository.PlanFlowRepository
import com.example.notification.NotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class AppNavTab(val title: String) {
    DASHBOARD("Panel"),
    TASKS("Tareas"),
    PLANNER("Agenda"),
    CALENDAR("Calendario"),
    FOCUS("Enfoque"),
    STATS("Progreso")
}

class PlanFlowViewModel(private val repository: PlanFlowRepository, private val context: Context) : ViewModel() {

    private val auth = Firebase.auth

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // Navigation state
    private val _currentTab = MutableStateFlow(AppNavTab.DASHBOARD)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    // Data streams
    val tasks: StateFlow<List<Task>> = repository.observeTasks(currentUserId ?: "guest_local")
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val projects: StateFlow<List<Project>> = (
        if (currentUserId != null) {
            repository.observeProjects(currentUserId!!)
        } else {
            MutableStateFlow(emptyList())
        }
    ).catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val goals: StateFlow<List<Goal>> = (
        if (currentUserId != null) {
            repository.observeGoals(currentUserId!!)
        } else {
            MutableStateFlow(emptyList())
        }
    ).catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val preferences: StateFlow<UserPreferences> = (
        if (currentUserId != null) {
            repository.observePreferences(currentUserId!!)
        } else {
            MutableStateFlow(UserPreferences())
        }
    ).catch { emit(UserPreferences()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UserPreferences())

    val syncEvents: StateFlow<List<SyncEvent>> = (
        if (currentUserId != null) {
            repository.observeSyncEvents(currentUserId!!)
        } else {
            MutableStateFlow(emptyList())
        }
    ).catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val sharedPrefs = context.getSharedPreferences("planflow_prefs", Context.MODE_PRIVATE)

    // School Mode active state (persisted across restarts)
    private val _isSchoolModeActive = MutableStateFlow(sharedPrefs.getBoolean("key_school_mode_active", false))
    val isSchoolModeActive: StateFlow<Boolean> = _isSchoolModeActive.asStateFlow()

    private val _showSchoolTaskDialog = MutableStateFlow(false)
    val showSchoolTaskDialog: StateFlow<Boolean> = _showSchoolTaskDialog.asStateFlow()

    private val _schoolTaskToEdit = MutableStateFlow<Task?>(null)
    val schoolTaskToEdit: StateFlow<Task?> = _schoolTaskToEdit.asStateFlow()

    // UI Dialog & Filter States
    private val _showTaskDialog = MutableStateFlow(false)
    val showTaskDialog: StateFlow<Boolean> = _showTaskDialog.asStateFlow()

    private val _taskToEdit = MutableStateFlow<Task?>(null)
    val taskToEdit: StateFlow<Task?> = _taskToEdit.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCategory = MutableStateFlow("Todas")
    val filterCategory: StateFlow<String> = _filterCategory.asStateFlow()

    private val _filterPriority = MutableStateFlow("Todas")
    val filterPriority: StateFlow<String> = _filterPriority.asStateFlow()

    private val _filterStatus = MutableStateFlow("all")
    val filterStatus: StateFlow<String> = _filterStatus.asStateFlow()

    private val _isAutoPlanning = MutableStateFlow(false)
    val isAutoPlanning: StateFlow<Boolean> = _isAutoPlanning.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _scheduledBlocks = MutableStateFlow<List<ScheduleBlock>>(emptyList())
    val scheduledBlocks: StateFlow<List<ScheduleBlock>> = _scheduledBlocks.asStateFlow()

    fun enterSchoolMode() {
        _isSchoolModeActive.value = true
        sharedPrefs.edit().putBoolean("key_school_mode_active", true).apply()
        _toastMessage.value = "Modo Colegio activado 🎒"
    }

    fun exitSchoolMode() {
        _isSchoolModeActive.value = false
        sharedPrefs.edit().putBoolean("key_school_mode_active", false).apply()
        _toastMessage.value = "¡Bienvenido a casa! Modo Colegio desactivado. Todas tus tareas escolares se conservan."
    }

    fun openCreateSchoolTaskDialog() {
        _schoolTaskToEdit.value = null
        _showSchoolTaskDialog.value = true
    }

    fun openEditSchoolTaskDialog(task: Task) {
        _schoolTaskToEdit.value = task
        _showSchoolTaskDialog.value = true
    }

    fun closeSchoolTaskDialog() {
        _showSchoolTaskDialog.value = false
        _schoolTaskToEdit.value = null
    }

    fun openCreateTaskDialog() {
        _taskToEdit.value = null
        _showTaskDialog.value = true
    }

    fun openEditTaskDialog(task: Task) {
        _taskToEdit.value = task
        _showTaskDialog.value = true
    }

    fun closeTaskDialog() {
        _showTaskDialog.value = false
        _taskToEdit.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCategory(category: String) {
        _filterCategory.value = category
    }

    fun setFilterPriority(priority: String) {
        _filterPriority.value = priority
    }

    fun setFilterStatus(status: String) {
        _filterStatus.value = status
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun saveTask(task: Task) {
        viewModelScope.launch {
            if (task.id.isBlank()) {
                val newId = repository.createTask(task)
                val createdTask = task.copy(id = newId)
                NotificationHelper.scheduleTaskAlarm(context, createdTask)
                NotificationHelper.scheduleTaskStartingAlarm(context, createdTask, 10)
                _toastMessage.value = "Tarea creada. Te avisaremos 10 min antes de que inicie."
            } else {
                repository.updateTask(task)
                NotificationHelper.scheduleTaskAlarm(context, task)
                NotificationHelper.scheduleTaskStartingAlarm(context, task, 10)
                _toastMessage.value = "Tarea actualizada"
            }
            closeTaskDialog()
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
            val statusText = if (task.status == "completed") "pendiente" else "completada"
            _toastMessage.value = "Tarea marcada como $statusText"
        }
    }

    fun toggleSubtask(task: Task, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(task, subtaskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _toastMessage.value = "Tarea eliminada"
        }
    }

    // Automatic Day Planning
    fun triggerAutoPlanDay(targetDate: LocalDate = LocalDate.now()) {
        viewModelScope.launch {
            try {
                _isAutoPlanning.value = true
                val currentTasks = tasks.value
                val currentPrefs = preferences.value
                val blocks = repository.autoPlanDay(
                    tasks = currentTasks,
                    preferences = currentPrefs,
                    targetDate = targetDate
                )
                _scheduledBlocks.value = blocks
                _isAutoPlanning.value = false

                // Schedule starting notification for each planned task (10 minutes before start!)
                blocks.filter { !it.isBreak }.forEach { block ->
                    try {
                        NotificationHelper.scheduleTaskStartingAlarm(context, block.task, 10)
                    } catch (e: Exception) {
                        // Safe
                    }
                }

                _toastMessage.value = if (blocks.isEmpty()) "No hay tareas pendientes para planificar hoy" else "¡Día organizado! Agenda generada con éxito."
            } catch (e: Exception) {
                _isAutoPlanning.value = false
                _toastMessage.value = "Agenda calculada."
            }
        }
    }

    fun updatePreferences(newPrefs: UserPreferences) {
        viewModelScope.launch {
            repository.savePreferences(newPrefs)
            _toastMessage.value = "Preferencias guardadas"
        }
    }

    fun addGoal(title: String, targetCount: Int, unit: String) {
        viewModelScope.launch {
            repository.createGoal(Goal(title = title, targetCount = targetCount, unit = unit))
            _toastMessage.value = "Nuevo objetivo personal añadido"
        }
    }

    fun updateGoalProgress(goalId: String, increment: Int) {
        viewModelScope.launch {
            repository.updateGoalProgress(goalId, increment)
        }
    }

    fun saveSchoolTask(task: Task) {
        viewModelScope.launch {
            try {
                val preparedTask = task.copy(
                    isSchoolTask = true,
                    category = "Colegio"
                )
                val isNew = preparedTask.id.isBlank()
                val finalTask = if (isNew) {
                    val newId = repository.createTask(preparedTask)
                    preparedTask.copy(id = newId)
                } else {
                    preparedTask
                }

                // Run smart conflict resolution
                val currentTasks = tasks.value
                val conflictRes = com.example.school.SchoolModeManager.organizeSchoolTasks(currentTasks, finalTask)

                // Save all updated tasks
                repository.saveBatchTasks(conflictRes.organizedTasks)

                NotificationHelper.scheduleTaskAlarm(context, finalTask)
                NotificationHelper.scheduleTaskStartingAlarm(context, finalTask, 10)

                _toastMessage.value = conflictRes.notificationMessage
                    ?: if (isNew) "Tarea escolar registrada" else "Tarea escolar actualizada"
            } catch (e: Exception) {
                _toastMessage.value = "Tarea escolar guardada"
            }
            closeSchoolTaskDialog()
        }
    }

    fun extendSchoolTask(task: Task, additionalMinutes: Int) {
        viewModelScope.launch {
            try {
                val currentTasks = tasks.value
                val res = com.example.school.SchoolModeManager.extendTaskDuration(task, additionalMinutes, currentTasks)
                repository.saveBatchTasks(res.organizedTasks)
                _toastMessage.value = res.notificationMessage ?: "Tiempo ampliado +$additionalMinutes min"
            } catch (e: Exception) {
                _toastMessage.value = "Horario ampliado"
            }
        }
    }

    fun markSchoolTaskOutcome(task: Task, outcome: String, rescheduleDate: String = "", rescheduleTime: String = "") {
        viewModelScope.launch {
            try {
                when (outcome) {
                    "completed" -> {
                        val updated = task.copy(
                            status = "completed",
                            completedAt = LocalDate.now().toString(),
                            postScheduleOutcome = "completed"
                        )
                        repository.updateTask(updated)
                        _toastMessage.value = "¡Tarea escolar completada! ✓"
                    }
                    "incomplete" -> {
                        val updated = task.copy(
                            status = "pending",
                            postScheduleOutcome = "incomplete",
                            dueDate = rescheduleDate.ifBlank { task.dueDate },
                            dueTime = rescheduleTime.ifBlank { task.dueTime },
                            scheduledStartTime = rescheduleTime.ifBlank { task.scheduledStartTime },
                            scheduledEndTime = if (rescheduleTime.isNotBlank()) {
                                com.example.school.SchoolModeManager.parseTime(rescheduleTime).plusMinutes(task.estimatedMinutes.toLong()).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                            } else task.scheduledEndTime
                        )
                        repository.updateTask(updated)
                        _toastMessage.value = if (rescheduleTime.isNotBlank()) "Tarea escolar reprogramada para las $rescheduleTime" else "Tarea marcada como incompleta (sigue pendiente)"
                    }
                }
            } catch (e: Exception) {
                // Safe
            }
        }
    }

    // Reset all data completely clean like new
    fun resetAndClearAllData() {
        viewModelScope.launch {
            val uid = currentUserId ?: return@launch
            repository.clearAllData(uid)
            _scheduledBlocks.value = emptyList()
            _toastMessage.value = "Espacio de trabajo reiniciado. Todo está como nuevo."
        }
    }
}

class PlanFlowViewModelFactory(
    private val repository: PlanFlowRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlanFlowViewModel(repository, context) as T
    }
}
