package com.reeled.quizoverlay.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.reeled.quizoverlay.R
import com.reeled.quizoverlay.ui.pin.PinActivity

object NotificationBuilder {
    private const val CHANNEL_ID = "quiz_overlay_service"
    private const val CHANNEL_NAME = "Learning Mode Service"
    
    const val ACTION_RESUME = "com.reeled.quizoverlay.ACTION_RESUME"
    const val ACTION_EXTEND = "com.reeled.quizoverlay.ACTION_EXTEND"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the quiz overlay active"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildActiveNotification(context: Context): Notification {
        val pauseIntent = Intent(context, PinActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        val pausePendingIntent = PendingIntent.getActivity(
            context, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Learning mode is on")
            .setContentText("Quizzes will pop up while you scroll.")
            .setSmallIcon(R.drawable.ic_brain)
            .setOngoing(true)
            .addAction(0, "Pause Quizzes", pausePendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildPausedNotification(context: Context, remainingMinutes: Int): Notification {
        val resumeIntent = Intent(context, com.reeled.quizoverlay.service.OverlayForegroundService::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getService(
            context, 1, resumeIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val extendIntent = Intent(context, com.reeled.quizoverlay.service.OverlayForegroundService::class.java).apply {
            action = ACTION_EXTEND
        }
        val extendPendingIntent = PendingIntent.getService(
            context, 2, extendIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Paused — $remainingMinutes min remaining")
            .setContentText("Quiz mode will resume automatically.")
            .setSmallIcon(R.drawable.ic_brain)
            .setOngoing(true)
            .addAction(0, "Resume Now", resumePendingIntent)
            .addAction(0, "Extend", extendPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
