package com.example.focus

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.notification.NotificationHelper

class FocusTimerService : Service() {

    companion object {
        const val NOTIFICATION_ID = 445566
        const val ACTION_START = "com.example.focus.ACTION_START"
        const val ACTION_STOP = "com.example.focus.ACTION_STOP"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TOTAL_SECONDS = "extra_total_seconds"

        fun startService(context: Context, taskTitle: String, totalSeconds: Int) {
            try {
                val intent = Intent(context, FocusTimerService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TASK_TITLE, taskTitle)
                    putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Prevent BackgroundServiceStartNotAllowedException on Android 12+
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, FocusTimerService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                try {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } catch (e: Exception) {
                    stopSelf()
                }
            }
            ACTION_START -> {
                val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Enfoque profundo"
                try {
                    val notification = buildOngoingNotification(taskTitle)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceCompat.startForeground(
                            this,
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (e: Exception) {
                    // Safe guard
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun buildOngoingNotification(taskTitle: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_FOCUS_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏱ Temporizador de Enfoque Activo")
            .setContentText("Trabajando en: $taskTitle • Ejecutándose en segundo plano")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
