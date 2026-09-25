package com.example.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "au_notes_alarm_channel"
        const val EXTRA_TITLE = "extra_alarm_title"
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Restore a pending alarm after a device reboot. The receiver is also
        // declared for BOOT_COMPLETED, so that event must not itself ring an alarm.
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("au_notes_prefs", Context.MODE_PRIVATE)
            val active = prefs.getBoolean("active_alarm", false)
            val trigger = prefs.getLong("alarm_time", 0L)
            if (active && trigger > System.currentTimeMillis()) {
                val title = prefs.getString("alarm_title", "Note Reminder Alarm") ?: "Note Reminder Alarm"
                val ringtone = prefs.getString("alarm_ringtone", "")?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                AlarmScheduler.scheduleAlarm(context, trigger, title, 0L, ringtone)
            }
            return
        }

        context.getSharedPreferences("au_notes_prefs", Context.MODE_PRIVATE).edit().putBoolean("active_alarm", false).apply()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Note Reminder Alarm"
        val customRingtoneUriStr = intent.getStringExtra(EXTRA_RINGTONE_URI)
        val ringtoneUri = if (!customRingtoneUriStr.isNullOrBlank()) {
            Uri.parse(customRingtoneUriStr)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Android O+ locks a notification channel's sound after creation. Use a
        // ringtone-specific channel so changing the selected alarm sound actually works.
        val channelId = "au_notes_alarm_" + (Integer.toHexString(customRingtoneUriStr?.hashCode() ?: 0))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                channelId,
                "AU Notes Alarms & Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms and reminders for AU Notes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                setSound(ringtoneUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_verified_badge)
            .setContentTitle(title)
            .setContentText("AU Notes Alarm: It's time for your scheduled reminder!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
