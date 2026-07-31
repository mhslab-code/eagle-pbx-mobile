package com.eaglesistemas.eaglepbx.telephony

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.Handler
import android.os.Looper
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
                    setSound(null, null)
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
        when (intent?.action) {
            ACTION_REJECT -> {
                onRejectIncoming?.invoke()
                cancelIncoming(this)
            }
        }
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
        private const val INCOMING_CHANNEL_ID = "eagle_pbx_incoming_calls_v2"
        private const val NOTIFICATION_ID = 101
        private const val INCOMING_NOTIFICATION_ID = 102
        const val ACTION_ANSWER =
            "com.eaglesistemas.eaglepbx.action.ANSWER_INCOMING_CALL"
        const val ACTION_SHOW_INCOMING =
            "com.eaglesistemas.eaglepbx.action.SHOW_INCOMING_CALL"
        private const val ACTION_REJECT =
            "com.eaglesistemas.eaglepbx.action.REJECT_INCOMING_CALL"

        @Volatile
        private var onRejectIncoming: (() -> Unit)? = null

        private var incomingRingtone: MediaPlayer? = null
        private val incomingTimeoutHandler = Handler(Looper.getMainLooper())
        private var incomingGeneration = 0L
        private var currentIncomingCallId = ""
        private val cancelledCallIds = mutableMapOf<String, Long>()

        fun setRejectCallHandler(onReject: (() -> Unit)?) {
            onRejectIncoming = onReject
        }

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

        fun showIncoming(
            context: Context,
            call: IncomingSipCall,
            callId: String = ""
        ) {
            val now = System.currentTimeMillis()
            synchronized(cancelledCallIds) {
                cancelledCallIds.entries.removeAll { now - it.value > 60_000L }
                if (callId.isNotBlank() && cancelledCallIds.remove(callId) != null) return
            }
            currentIncomingCallId = callId
            val generation = ++incomingGeneration
            stopIncomingRingtone()
            val ringtoneUri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            incomingRingtone = MediaPlayer().also { player ->
                runCatching {
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    player.setDataSource(context, ringtoneUri)
                    player.isLooping = true
                    player.setOnPreparedListener { prepared ->
                        if (incomingRingtone === prepared) prepared.start()
                        else prepared.release()
                    }
                    player.setOnErrorListener { failed, _, _ ->
                        if (incomingRingtone === failed) incomingRingtone = null
                        failed.release()
                        true
                    }
                    player.prepareAsync()
                }.onFailure {
                    if (incomingRingtone === player) incomingRingtone = null
                    player.release()
                }
            }
            val openApp = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_SHOW_INCOMING
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val answerCall = PendingIntent.getActivity(
                context,
                2,
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_ANSWER
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val rejectCall = PendingIntent.getService(
                context,
                3,
                Intent(context, SipForegroundService::class.java).apply {
                    action = ACTION_REJECT
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
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "Recusar",
                    rejectCall
                )
                .addAction(
                    R.drawable.ic_launcher_foreground,
                    "Atender",
                    answerCall
                )
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(INCOMING_NOTIFICATION_ID, notification)
            incomingTimeoutHandler.postDelayed({
                if (generation == incomingGeneration) cancelIncoming(context)
            }, 45_000L)
        }

        fun cancelIncoming(context: Context, callId: String = "") {
            if (callId.isNotBlank()) {
                synchronized(cancelledCallIds) {
                    cancelledCallIds[callId] = System.currentTimeMillis()
                }
                if (currentIncomingCallId.isNotBlank() && currentIncomingCallId != callId) {
                    return
                }
            }
            incomingGeneration += 1
            currentIncomingCallId = ""
            stopIncomingRingtone()
            context.getSystemService(NotificationManager::class.java)
                .cancel(INCOMING_NOTIFICATION_ID)
        }

        private fun stopIncomingRingtone() {
            incomingRingtone?.runCatching {
                if (isPlaying) stop()
                reset()
                release()
            }
            incomingRingtone = null
        }
    }
}
