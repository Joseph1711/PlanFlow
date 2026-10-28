package com.example.focus

import android.content.Context
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FocusTimerState(
    val isRunning: Boolean = false,
    val mode: String = "work", // work, short_break, long_break
    val totalSeconds: Int = 25 * 60,
    val secondsRemaining: Int = 25 * 60,
    val taskTitle: String = "Sesión de Trabajo",
    val endTimeMs: Long = 0L,
    val completedSessions: Int = 0
)

object FocusTimerManager {

    private val _timerState = MutableStateFlow(FocusTimerState())
    val timerState: StateFlow<FocusTimerState> = _timerState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null

    fun startTimer(context: Context, mode: String, totalSeconds: Int, taskTitle: String) {
        val current = _timerState.value
        val duration = if (current.mode == mode && current.secondsRemaining > 0 && current.secondsRemaining < totalSeconds) {
            current.secondsRemaining
        } else {
            totalSeconds
        }

        val targetEnd = System.currentTimeMillis() + (duration * 1000L)

        _timerState.value = current.copy(
            isRunning = true,
            mode = mode,
            totalSeconds = totalSeconds,
            secondsRemaining = duration,
            taskTitle = taskTitle,
            endTimeMs = targetEnd
        )

        // Schedule exact Alarm for completion
        NotificationHelper.scheduleTimerCompleteAlarm(context, taskTitle, targetEnd)

        // Start Foreground Service for background notification
        FocusTimerService.startService(context, taskTitle, duration)

        startLocalTicker(context)
    }

    fun pauseTimer(context: Context) {
        tickerJob?.cancel()
        val current = _timerState.value
        val now = System.currentTimeMillis()
        val remaining = if (current.endTimeMs > now) {
            ((current.endTimeMs - now) / 1000L).toInt()
        } else {
            0
        }

        _timerState.value = current.copy(
            isRunning = false,
            secondsRemaining = remaining
        )

        NotificationHelper.cancelTimerAlarm(context)
        FocusTimerService.stopService(context)
    }

    fun resetTimer(context: Context) {
        tickerJob?.cancel()
        val current = _timerState.value
        _timerState.value = current.copy(
            isRunning = false,
            secondsRemaining = current.totalSeconds,
            endTimeMs = 0L
        )

        NotificationHelper.cancelTimerAlarm(context)
        FocusTimerService.stopService(context)
    }

    fun skipInterval(context: Context) {
        tickerJob?.cancel()
        NotificationHelper.cancelTimerAlarm(context)
        FocusTimerService.stopService(context)
        onTimerCompleted()
    }

    fun onTimerCompleted() {
        tickerJob?.cancel()
        val current = _timerState.value
        if (current.mode == "work") {
            val newCompleted = current.completedSessions + 1
            val nextMode = if (newCompleted % 4 == 0) "long_break" else "short_break"
            val nextSeconds = if (nextMode == "long_break") 15 * 60 else 5 * 60
            _timerState.value = current.copy(
                isRunning = false,
                mode = nextMode,
                totalSeconds = nextSeconds,
                secondsRemaining = nextSeconds,
                completedSessions = newCompleted,
                endTimeMs = 0L
            )
        } else {
            val nextSeconds = 25 * 60
            _timerState.value = current.copy(
                isRunning = false,
                mode = "work",
                totalSeconds = nextSeconds,
                secondsRemaining = nextSeconds,
                endTimeMs = 0L
            )
        }
    }

    // Called when user opens the screen to re-sync remaining time
    fun syncWithClock() {
        val current = _timerState.value
        if (current.isRunning && current.endTimeMs > 0) {
            val now = System.currentTimeMillis()
            val remaining = if (current.endTimeMs > now) {
                ((current.endTimeMs - now) / 1000L).toInt()
            } else {
                0
            }
            if (remaining <= 0) {
                onTimerCompleted()
            } else {
                _timerState.value = current.copy(secondsRemaining = remaining)
            }
        }
    }

    private fun startLocalTicker(context: Context) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (_timerState.value.isRunning) {
                delay(1000L)
                val current = _timerState.value
                if (!current.isRunning) break

                val now = System.currentTimeMillis()
                val remaining = if (current.endTimeMs > now) {
                    ((current.endTimeMs - now) / 1000L).toInt()
                } else {
                    0
                }

                if (remaining <= 0) {
                    onTimerCompleted()
                    FocusTimerService.stopService(context)
                    break
                } else {
                    _timerState.value = current.copy(secondsRemaining = remaining)
                }
            }
        }
    }
}
