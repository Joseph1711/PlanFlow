package com.example.focus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.notification.NotificationHelper

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Sesión de concentración"

        // Stop foreground service if running
        FocusTimerService.stopService(context)
        FocusTimerManager.onTimerCompleted()

        // Vibrate
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Show Heads-Up Notification
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            NotificationHelper.TIMER_ALARM_REQ_CODE + 1,
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 ¡Sesión de Concentración Finalizada!")
            .setContentText("Has completado tu bloque de trabajo en: $taskTitle. ¡Excelente trabajo, tómate un descanso!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Has completado tu bloque de trabajo en: $taskTitle. ¡Excelente trabajo, tómate un merecido descanso!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NotificationHelper.TIMER_ALARM_REQ_CODE, notification)
        } catch (e: SecurityException) {
            // Permission
        }
    }
}
