package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskStartingAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(NotificationHelper.EXTRA_TASK_ID) ?: return
        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Tu tarea"
        val startTime = intent.getStringExtra(NotificationHelper.EXTRA_START_TIME) ?: "pronto"

        NotificationHelper.showTaskStartingNotification(
            context = context,
            taskId = taskId,
            taskTitle = taskTitle,
            startTime = startTime,
            minutesBefore = 10
        )
    }
}
