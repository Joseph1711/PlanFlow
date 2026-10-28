package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Tarea programada"

        NotificationHelper.showTaskReminderNotification(
            context = context,
            taskId = taskId,
            taskTitle = taskTitle,
            message = "¡Es hora de realizar tu tarea programada! Pulsa para ver o marca como completada."
        )
    }
}
