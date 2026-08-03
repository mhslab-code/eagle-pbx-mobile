package com.eaglesistemas.eaglepbx.telephony

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.eaglesistemas.eaglepbx.MainActivity
import com.eaglesistemas.eaglepbx.IncomingCallActivity
import com.eaglesistemas.eaglepbx.R
import com.eaglesistemas.eaglepbx.data.EagleContact
import com.eaglesistemas.eaglepbx.data.SecureSessionStore

internal enum class IncomingAlertMode {
    SOUND,
    VIBRATE,
    SILENT
}

internal fun incomingAlertMode(ringerMode: Int?): IncomingAlertMode = when (ringerMode) {
    AudioManager.RINGER_MODE_NORMAL -> IncomingAlertMode.SOUND
    AudioManager.RINGER_MODE_VIBRATE -> IncomingAlertMode.VIBRATE
    else -> IncomingAlertMode.SILENT
}

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
                    enableVibration(false)
                }
            )
            deleteNotificationChannel(LEGACY_INCOMING_CHANNEL_ID_V4)
            deleteNotificationChannel(LEGACY_INCOMING_CHANNEL_ID)
            deleteNotificationChannel(LEGACY_INCOMING_CHANNEL_ID_V2)
            createNotificationChannel(
                NotificationChannel(
                    MISSED_CHANNEL_ID,
                    "Chamadas perdidas",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Mantém o aviso de chamadas não atendidas."
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
                markRejected(this)
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
            .setSmallIcon(R.drawable.eagle_pbx_logo_official)
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
        const val INCOMING_CHANNEL_ID = "eagle_pbx_incoming_calls_v5"
        private const val LEGACY_INCOMING_CHANNEL_ID_V4 = "eagle_pbx_incoming_calls_v4"
        private const val LEGACY_INCOMING_CHANNEL_ID = "eagle_pbx_incoming_calls_v3"
        private const val LEGACY_INCOMING_CHANNEL_ID_V2 = "eagle_pbx_incoming_calls_v2"
        private const val MISSED_CHANNEL_ID = "eagle_pbx_missed_calls"
        private const val NOTIFICATION_ID = 101
        private const val INCOMING_NOTIFICATION_ID = 102
        const val ACTION_ANSWER =
            "com.eaglesistemas.eaglepbx.action.ANSWER_INCOMING_CALL"
        const val ACTION_SHOW_INCOMING =
            "com.eaglesistemas.eaglepbx.action.SHOW_INCOMING_CALL"
        const val EXTRA_CALL_ID = "eagle_pbx_call_id"
        const val EXTRA_CALLER_NUMBER = "eagle_pbx_caller_number"
        const val EXTRA_CALLER_NAME = "eagle_pbx_caller_name"
        const val EXTRA_CALLER_PHOTO = "eagle_pbx_caller_photo"
        const val ACTION_REJECT =
            "com.eaglesistemas.eaglepbx.action.REJECT_INCOMING_CALL"

        @Volatile
        private var onRejectIncoming: (() -> Unit)? = null

        @Volatile
        private var onIncomingNotificationChanged: ((IncomingSipCall?) -> Unit)? = null

        @Volatile
        private var activeIncomingCall: IncomingSipCall? = null

        @Volatile
        private var applicationVisible = false

        private val incomingRingtoneLock = Any()
        private var incomingRingtone: MediaPlayer? = null
        private val incomingVibrationLock = Any()
        private var incomingVibrator: Vibrator? = null
        private val incomingWakeLockLock = Any()
        private var incomingWakeLock: PowerManager.WakeLock? = null
        private val incomingTimeoutHandler = Handler(Looper.getMainLooper())
        private var incomingGeneration = 0L
        private var currentIncomingCallId = ""
        private var incomingDisposition = IncomingDisposition.NONE
        private val cancelledCallIds = mutableMapOf<String, Long>()

        private enum class IncomingDisposition {
            NONE,
            RINGING,
            ANSWERED,
            REJECTED
        }

        fun setRejectCallHandler(onReject: (() -> Unit)?) {
            onRejectIncoming = onReject
        }

        fun setIncomingNotificationHandler(
            handler: ((IncomingSipCall?) -> Unit)?
        ) {
            onIncomingNotificationChanged = handler
            handler?.invoke(activeIncomingCall)
        }

        fun currentIncomingCall(): IncomingSipCall? = activeIncomingCall

        fun setApplicationVisible(context: Context, visible: Boolean) {
            applicationVisible = visible
            val notificationManager =
                context.getSystemService(NotificationManager::class.java)
            if (visible) {
                notificationManager.cancel(INCOMING_NOTIFICATION_ID)
            } else {
                activeIncomingCall?.let { call ->
                    showIncomingNotification(context, call, currentIncomingCallId)
                }
            }
        }

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SipForegroundService::class.java)
            )
        }

        fun stop(context: Context) {
            cancelIncoming(context, showMissed = false)
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
            val directory = cachedCaller(context, call.number)
            val caller = directory?.name
                ?: call.displayName?.takeIf(String::isNotBlank)
                ?: formatPhoneNumber(call.number)
            val effectiveCall = IncomingSipCall(
                number = call.number,
                displayName = caller
            )
            activeIncomingCall = effectiveCall
            incomingDisposition = IncomingDisposition.RINGING
            onIncomingNotificationChanged?.invoke(effectiveCall)
            val generation = ++incomingGeneration
            ensureIncomingAlert(context)
            if (!applicationVisible) {
                wakeScreenForIncomingCall(context)
                showIncomingNotification(context, effectiveCall, callId)
            }
            incomingTimeoutHandler.postDelayed({
                if (generation == incomingGeneration) {
                    cancelIncoming(context, showMissed = true)
                }
            }, 45_000L)
        }

        private fun showIncomingNotification(
            context: Context,
            call: IncomingSipCall,
            callId: String
        ) {
            val directory = cachedCaller(context, call.number)
            val caller = call.displayName
                ?.takeIf(String::isNotBlank)
                ?: directory?.name
                ?: formatPhoneNumber(call.number)
            val openApp = PendingIntent.getActivity(
                context,
                1,
                Intent(context, IncomingCallActivity::class.java).apply {
                    action = ACTION_SHOW_INCOMING
                    putExtra(EXTRA_CALL_ID, callId)
                    putExtra(EXTRA_CALLER_NUMBER, call.number)
                    putExtra(EXTRA_CALLER_NAME, caller)
                    putExtra(EXTRA_CALLER_PHOTO, directory?.photo)
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
                    putExtra(EXTRA_CALL_ID, callId)
                    putExtra(EXTRA_CALLER_NUMBER, call.number)
                    putExtra(EXTRA_CALLER_NAME, caller)
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
            val formattedNumber = formatPhoneNumber(call.number)
            val appIcon = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.eagle_pbx_logo
            )
            val callerPhoto = decodeDataImage(directory?.photo)
            val largeIcon = if (callerPhoto != null) {
                circularBitmapWithBadge(callerPhoto, appIcon)
            } else {
                circularBitmap(appIcon)
            }
            val callerPerson = Person.Builder()
                .setName(caller)
                .setIcon(IconCompat.createWithBitmap(largeIcon))
                .setImportant(true)
                .build()
            val notification = NotificationCompat.Builder(context, INCOMING_CHANNEL_ID)
                .setSmallIcon(R.drawable.eagle_pbx_logo_official)
                .setContentTitle(caller)
                .setContentText(
                    if (directory != null && formattedNumber != caller) formattedNumber
                    else "Chamada recebida"
                )
                .setSubText("Eagle PBX")
                .setContentIntent(openApp)
                .setFullScreenIntent(openApp, true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setStyle(
                    NotificationCompat.CallStyle.forIncomingCall(
                        callerPerson,
                        rejectCall,
                        answerCall
                    )
                )
                .build()
            context.getSystemService(NotificationManager::class.java)
                .notify(INCOMING_NOTIFICATION_ID, notification)
            if (context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true) {
                runCatching { openApp.send() }
            }
        }

        private fun ensureIncomingRingtone(context: Context) {
            synchronized(incomingRingtoneLock) {
                if (incomingRingtone != null) return
                val player = MediaPlayer()
                incomingRingtone = player
                runCatching {
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    context.resources.openRawResourceFd(R.raw.eagle_pbx_ringtone).use { sound ->
                        player.setDataSource(
                            sound.fileDescriptor,
                            sound.startOffset,
                            sound.length
                        )
                    }
                    player.isLooping = true
                    player.setOnPreparedListener { prepared ->
                        val active = synchronized(incomingRingtoneLock) {
                            incomingRingtone === prepared
                        }
                        if (active) {
                            runCatching { prepared.start() }
                                .onFailure {
                                    synchronized(incomingRingtoneLock) {
                                        if (incomingRingtone === prepared) {
                                            incomingRingtone = null
                                        }
                                    }
                                    prepared.release()
                                }
                        } else {
                            prepared.release()
                        }
                    }
                    player.setOnErrorListener { failed, _, _ ->
                        synchronized(incomingRingtoneLock) {
                            if (incomingRingtone === failed) incomingRingtone = null
                        }
                        failed.release()
                        true
                    }
                    player.prepareAsync()
                }.onFailure {
                    if (incomingRingtone === player) incomingRingtone = null
                    player.release()
                }
            }
        }

        private fun ensureIncomingAlert(context: Context) {
            val audioManager = context.getSystemService(AudioManager::class.java)
            when (incomingAlertMode(audioManager?.ringerMode)) {
                IncomingAlertMode.SOUND -> {
                    stopIncomingVibration()
                    ensureIncomingRingtone(context)
                }
                IncomingAlertMode.VIBRATE -> {
                    stopIncomingRingtone()
                    ensureIncomingVibration(context)
                }
                IncomingAlertMode.SILENT -> {
                    stopIncomingRingtone()
                    stopIncomingVibration()
                }
            }
        }

        private fun ensureIncomingVibration(context: Context) {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return
            if (!vibrator.hasVibrator()) return
            synchronized(incomingVibrationLock) {
                if (incomingVibrator != null) return
                incomingVibrator = vibrator
            }
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 650L, 350L, 650L, 1_200L),
                    0
                )
            )
        }

        private fun stopIncomingVibration() {
            val vibrator = synchronized(incomingVibrationLock) {
                incomingVibrator.also { incomingVibrator = null }
            }
            vibrator?.cancel()
        }

        private fun stopIncomingAlert() {
            stopIncomingRingtone()
            stopIncomingVibration()
            releaseIncomingWakeLock()
        }

        @Suppress("DEPRECATION")
        private fun wakeScreenForIncomingCall(context: Context) {
            val powerManager = context.getSystemService(PowerManager::class.java)
                ?: return
            if (powerManager.isInteractive) return
            synchronized(incomingWakeLockLock) {
                incomingWakeLock?.let { wakeLock ->
                    if (wakeLock.isHeld) wakeLock.release()
                }
                incomingWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "EaglePBX:IncomingCall"
                ).apply {
                    setReferenceCounted(false)
                    acquire(15_000L)
                }
            }
        }

        private fun releaseIncomingWakeLock() {
            synchronized(incomingWakeLockLock) {
                incomingWakeLock?.let { wakeLock ->
                    if (wakeLock.isHeld) wakeLock.release()
                }
                incomingWakeLock = null
            }
        }

        fun markAnswered(context: Context) {
            incomingDisposition = IncomingDisposition.ANSWERED
            cancelIncoming(context, showMissed = false)
        }

        /**
         * Stops the user-facing alert while Linphone negotiates the answer, but keeps the
         * incoming call available until the SIP state actually changes to CONNECTED.
         */
        fun prepareForAnswer(context: Context) {
            incomingDisposition = IncomingDisposition.ANSWERED
            stopIncomingAlert()
            context.getSystemService(NotificationManager::class.java)
                .cancel(INCOMING_NOTIFICATION_ID)
        }

        fun markRejected(context: Context) {
            incomingDisposition = IncomingDisposition.REJECTED
            cancelIncoming(context, showMissed = false)
        }

        fun cancelIncoming(
            context: Context,
            callId: String = "",
            showMissed: Boolean? = null
        ) {
            if (callId.isNotBlank()) {
                synchronized(cancelledCallIds) {
                    cancelledCallIds[callId] = System.currentTimeMillis()
                }
                if (currentIncomingCallId.isNotBlank() && currentIncomingCallId != callId) {
                    return
                }
            }
            val missedCall = activeIncomingCall
            val shouldShowMissed = showMissed
                ?: (incomingDisposition == IncomingDisposition.RINGING)
            incomingGeneration += 1
            currentIncomingCallId = ""
            activeIncomingCall = null
            incomingDisposition = IncomingDisposition.NONE
            onIncomingNotificationChanged?.invoke(null)
            stopIncomingAlert()
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.cancel(INCOMING_NOTIFICATION_ID)
            if (shouldShowMissed && missedCall != null) {
                showMissedCall(context, manager, missedCall)
            }
        }

        private fun showMissedCall(
            context: Context,
            manager: NotificationManager,
            call: IncomingSipCall
        ) {
            val openApp = PendingIntent.getActivity(
                context,
                4,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val directory = cachedCaller(context, call.number)
            val caller = directory?.name
                ?: call.displayName?.takeIf(String::isNotBlank)
                ?: formatPhoneNumber(call.number)
            val appIcon = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.eagle_pbx_logo
            )
            val callerPhoto = decodeDataImage(directory?.photo)
            val largeIcon = if (callerPhoto != null) {
                circularBitmapWithBadge(callerPhoto, appIcon)
            } else {
                circularBitmap(appIcon)
            }
            val notification = NotificationCompat.Builder(context, MISSED_CHANNEL_ID)
                .setSmallIcon(R.drawable.eagle_pbx_logo_official)
                .setLargeIcon(largeIcon)
                .setContentTitle(caller)
                .setContentText("Chamada perdida")
                .setSubText("Eagle PBX")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .build()
            manager.notify(INCOMING_NOTIFICATION_ID, notification)
        }

        private fun stopIncomingRingtone() {
            val player = synchronized(incomingRingtoneLock) {
                incomingRingtone.also { incomingRingtone = null }
            }
            player?.runCatching {
                if (isPlaying) stop()
                reset()
                release()
            }
        }

        private fun cachedCaller(context: Context, number: String): EagleContact? {
            val store = SecureSessionStore(context.applicationContext)
            val extension = store.readUser()?.extension.orEmpty()
            if (extension.isBlank()) return null
            val incoming = number.filter(Char::isDigit)
            if (incoming.isBlank()) return null
            return store.readContacts(extension).orEmpty().firstOrNull { contact ->
                contact.numbers.any { entry ->
                    val stored = entry.number.filter(Char::isDigit)
                    stored == incoming ||
                        (stored.length >= 8 && incoming.length >= 8 &&
                            (stored.endsWith(incoming) || incoming.endsWith(stored)))
                }
            }
        }

        private fun decodeDataImage(value: String?): Bitmap? {
            val encoded = value
                ?.takeIf { it.startsWith("data:image/") && it.contains(',') }
                ?.substringAfter(',')
                ?: return null
            return runCatching {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull()
        }

        private fun circularBitmap(source: Bitmap): Bitmap {
            val size = minOf(source.width, source.height)
            val left = (source.width - size) / 2f
            val top = (source.height - size) / 2f
            val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply { setTranslate(-left, -top) })
            }
            Canvas(output).drawCircle(
                size / 2f,
                size / 2f,
                size / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            )
            return output
        }

        private fun circularBitmapWithBadge(source: Bitmap, badge: Bitmap): Bitmap {
            val output = circularBitmap(source)
            val canvas = Canvas(output)
            val badgeRadius = output.width * 0.18f
            val centerX = output.width - badgeRadius
            val centerY = output.height - badgeRadius
            canvas.drawCircle(
                centerX,
                centerY,
                badgeRadius * 1.12f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
            )
            val diameter = badgeRadius * 2f
            val scale = maxOf(diameter / badge.width, diameter / badge.height)
            val shader = BitmapShader(badge, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
                setLocalMatrix(Matrix().apply {
                    setScale(scale, scale)
                    postTranslate(
                        centerX - badge.width * scale / 2f,
                        centerY - badge.height * scale / 2f
                    )
                })
            }
            canvas.drawCircle(
                centerX,
                centerY,
                badgeRadius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            )
            return output
        }

        private fun formatPhoneNumber(value: String): String {
            val trimmed = value.trim()
            val digits = trimmed.filter(Char::isDigit)
            return when {
                digits.isBlank() -> "Número não identificado"
                trimmed.startsWith("+") && digits.length == 13 ->
                    "+${digits.take(2)} (${digits.substring(2, 4)}) " +
                        "${digits.substring(4, 9)}-${digits.substring(9)}"
                digits.length == 11 ->
                    "(${digits.take(2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
                digits.length == 10 ->
                    "(${digits.take(2)}) ${digits.substring(2, 6)}-${digits.substring(6)}"
                else -> trimmed.ifBlank { digits }
            }
        }
    }
}
