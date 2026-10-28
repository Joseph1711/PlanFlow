package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.repository.PlanFlowRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Tarea"

        // Dismiss the current notification
        NotificationManagerCompat.from(context).cancel(taskId.hashCode())

        when (intent.action) {
            NotificationHelper.ACTION_COMPLETE -> {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    val repository = PlanFlowRepository(context)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val dummyTask = com.example.data.model.Task(id = taskId, title = taskTitle, status = "completed")
                            repository.updateTask(dummyTask, "Acción rápida Notificación")
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                }
            }
            NotificationHelper.ACTION_SNOOZE -> {
                // Reschedule for 15 minutes later
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val snoozeTime = System.currentTimeMillis() + 15 * 60 * 1000

                val alarmIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                    putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
                    putExtra(NotificationHelper.EXTRA_TASK_TITLE, taskTitle)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    taskId.hashCode() + 999,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)
            }
        }
    }
}
