package com.eaglesistemas.eaglepbx.telephony

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.eaglesistemas.eaglepbx.MainActivity
import com.eaglesistemas.eaglepbx.R

/**
 * Keeps the authenticated SIP process eligible to run while the activity is
 * in the background. Process-death recovery belongs to the push phase.
 */
class SipForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Telefonia Eagle PBX",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Mantém o ramal disponível em segundo plano."
                    setShowBadge(false)
                }
            )
            createNotificationChannel(
                NotificationChannel(
                    INCOMING_CHANNEL_ID,
                    "Chamadas recebidas",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisa sobre novas chamadas do Eagle PBX."
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                        Notification.AUDIO_ATTRIBUTES_DEFAULT
                    )
                    enableVibration(true)
                }
            )
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        startForeground(NOTIFICATION_ID, notification())
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Eagle PBX online")
            .setContentText("Ramal disponível para receber chamadas.")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "eagle_pbx_telephony"
        private const val INCOMING_CHANNEL_ID = "eagle_pbx_incoming_calls"
        private const val NOTIFICATION_ID = 101
        private const val INCOMING_NOTIFICATION_ID = 102

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SipForegroundService::class.java)
            )
        }

        fun stop(context: Context) {
            cancelIncoming(context)
            context.stopService(
                Intent(context, SipForegroundService::class.java)
            )
        }

        fun showIncoming(context: Context, call: IncomingSipCall) {
            val openApp = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val caller = call.displayName?.takeIf(String::isNotBlank)
                ?: call.number
                .takeIf(String::isNotBlank)
                ?: "Número não identificado"
            val notification = NotificationCompat.Builder(context, INCOMING_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Chamada recebida")
                .setContentText(caller)
                .setContentIntent(openApp)
                .setFullScreenIntent(openApp, true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(INCOMING_NOTIFICATION_ID, notification)
        }

        fun cancelIncoming(context: Context) {
            context.getSystemService(NotificationManager::class.java)
                .cancel(INCOMING_NOTIFICATION_ID)
        }
    }
}
