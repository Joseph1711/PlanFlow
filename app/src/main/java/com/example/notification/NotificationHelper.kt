package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Task
import com.example.focus.TimerAlarmReceiver
import java.util.Calendar

object NotificationHelper {

    const val CHANNEL_REMINDERS = "channel_planflow_reminders"
    const val CHANNEL_TASK_STARTING = "channel_planflow_task_starting"
    const val CHANNEL_DAY_SUMMARY = "channel_planflow_summary"
    const val CHANNEL_FOCUS_TIMER = "channel_planflow_focus_timer"

    const val ACTION_COMPLETE = "com.example.planflow.ACTION_COMPLETE_TASK"
    const val ACTION_SNOOZE = "com.example.planflow.ACTION_SNOOZE_TASK"
    const val ACTION_START_FOCUS = "com.example.planflow.ACTION_START_FOCUS"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_START_TIME = "extra_start_time"

    const val TIMER_ALARM_REQ_CODE = 998877

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones inteligentes de tareas y fechas límite"
                enableVibration(true)
            }

            val startingChannel = NotificationChannel(
                CHANNEL_TASK_STARTING,
                "Avisos de Inicio de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos previos cuando una tarea está por comenzar"
                enableVibration(true)
            }

            val summaryChannel = NotificationChannel(
                CHANNEL_DAY_SUMMARY,
                "Resumen Diario de Agenda",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resumen de planificación y agenda del día"
            }

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_TIMER,
                "Temporizador de Concentración",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Progreso del temporizador de enfoque en segundo plano"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(reminderChannel, startingChannel, summaryChannel, focusChannel)
            )
        }
    }

    // Notification for task due/reminder
    fun showTaskReminderNotification(context: Context, taskId: String, taskTitle: String, message: String, notificationId: Int = taskId.hashCode()) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("PlanFlow: $taskTitle")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "✓ Completar", completePendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "⏱ Posponer 15m", snoozePendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    // Notification when a task is about to start!
    fun showTaskStartingNotification(
        context: Context,
        taskId: String,
        taskTitle: String,
        startTime: String,
        minutesBefore: Int = 10
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ACTION_START_FOCUS, true)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, taskTitle)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode() + 500,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = "Tu tarea '$taskTitle' está por comenzar a las $startTime (en $minutesBefore minutos). ¡Prepárate para empezar!"

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_STARTING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Tarea por iniciar: $taskTitle")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "🎯 Iniciar en Enfoque", openAppPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(taskId.hashCode() + 500, notification)
        } catch (e: SecurityException) {
            // Permission
        }
    }

    // Schedule alarm for when a task is about to start
    fun scheduleTaskStartingAlarm(context: Context, task: Task, minutesBeforeStart: Int = 10) {
        val dateStr = if (task.scheduledDate.isNotBlank()) task.scheduledDate else task.dueDate
        val timeStr = if (task.scheduledStartTime.isNotBlank()) task.scheduledStartTime else task.dueTime

        if (dateStr.isBlank() || timeStr.isBlank()) return

        try {
            val dateParts = dateStr.split("-")
            val timeParts = timeStr.split(":")
            if (dateParts.size == 3 && timeParts.size >= 2) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, dateParts[0].toInt())
                    set(Calendar.MONTH, dateParts[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                }

                // Subtract minutesBeforeStart
                calendar.add(Calendar.MINUTE, -minutesBeforeStart)

                if (calendar.timeInMillis > System.currentTimeMillis()) {
                    val intent = Intent(context, TaskStartingAlarmReceiver::class.java).apply {
                        putExtra(EXTRA_TASK_ID, task.id)
                        putExtra(EXTRA_TASK_TITLE, task.title)
                        putExtra(EXTRA_START_TIME, timeStr)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        (task.id + "_start").hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors safely
        }
    }

    // Schedule regular task deadline alarm
    fun scheduleTaskAlarm(context: Context, task: Task) {
        if (task.dueDate.isBlank() || task.dueTime.isBlank()) return

        try {
            val parts = task.dueDate.split("-")
            val timeParts = task.dueTime.split(":")
            if (parts.size == 3 && timeParts.size >= 2) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, parts[0].toInt())
                    set(Calendar.MONTH, parts[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                    set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    set(Calendar.MINUTE, timeParts[1].toInt())
                    set(Calendar.SECOND, 0)
                }

                calendar.add(Calendar.MINUTE, -task.reminderMinutesBefore)

                if (calendar.timeInMillis > System.currentTimeMillis()) {
                    val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
                        putExtra(EXTRA_TASK_ID, task.id)
                        putExtra(EXTRA_TASK_TITLE, task.title)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        task.id.hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Also schedule "task starting" alert!
        scheduleTaskStartingAlarm(context, task, minutesBeforeStart = 10)
    }

    // Schedule Timer Completion Alarm
    fun scheduleTimerCompleteAlarm(context: Context, taskTitle: String, triggerAtMillis: Long) {
        try {
            val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
                putExtra(EXTRA_TASK_TITLE, taskTitle)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                TIMER_ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (e: Exception) {
            // Safe guard against alarm policy restrictions
        }
    }

    fun cancelTimerAlarm(context: Context) {
        try {
            val intent = Intent(context, TimerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                TIMER_ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
