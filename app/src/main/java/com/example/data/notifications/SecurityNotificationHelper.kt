package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object SecurityNotificationHelper {

    const val CHANNEL_SECURITY_ALERTS = "scamshield_security_alerts"
    const val CHANNEL_FAMILY_WARNINGS = "scamshield_family_warnings"
    const val CHANNEL_SCAM_TRENDS = "scamshield_scam_trends"
    const val CHANNEL_EDUCATIONAL = "scamshield_educational"

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_SECURITY_ALERTS,
                    "Security & High-Risk Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical scam alerts and high-risk scan warnings."
                },
                NotificationChannel(
                    CHANNEL_FAMILY_WARNINGS,
                    "Family Shared Warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Privacy-safe scam warnings shared by family members."
                },
                NotificationChannel(
                    CHANNEL_SCAM_TRENDS,
                    "Regional Scam Trends",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Important scam trends in your selected country."
                },
                NotificationChannel(
                    CHANNEL_EDUCATIONAL,
                    "Safety Tips & Lessons",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Optional cybersecurity learning reminders."
                }
            )
            manager.createNotificationChannels(channels)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showSecurityAlertNotification(
        context: Context,
        channelId: String,
        title: String,
        body: String,
        notificationId: Int = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    ) {
        if (!hasNotificationPermission(context)) return
        ensureNotificationChannels(context)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify
        }
    }
}
