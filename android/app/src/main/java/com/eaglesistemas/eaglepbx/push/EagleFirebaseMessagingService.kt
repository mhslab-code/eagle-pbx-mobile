package com.eaglesistemas.eaglepbx.push

import com.eaglesistemas.eaglepbx.EaglePbxApplication
import com.eaglesistemas.eaglepbx.data.DeviceIdentityStore
import com.eaglesistemas.eaglepbx.data.EagleApiClient
import com.eaglesistemas.eaglepbx.data.SecureSessionStore
import com.eaglesistemas.eaglepbx.telephony.IncomingSipCall
import com.eaglesistemas.eaglepbx.telephony.SipForegroundService
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class EagleFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        val sessionStore = SecureSessionStore(applicationContext)
        if (sessionStore.read().isNullOrBlank()) return

        Thread {
            runCatching {
                EagleApiClient(
                    sessionStore,
                    DeviceIdentityStore(applicationContext)
                ).updatePushRegistration(installationId)
            }
        }.start()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val eventType = message.data["type"]
        val callId = message.data["callId"].orEmpty().take(160)
        if (eventType == "incoming_call_cancelled") {
            SipForegroundService.cancelIncoming(applicationContext, callId)
            return
        }
        if (eventType != "incoming_call") return
        val sessionStore = SecureSessionStore(applicationContext)
        if (sessionStore.read().isNullOrBlank()) return

        (applicationContext as? EaglePbxApplication)
            ?.loginViewModel
            ?.processIncomingPush()
        val callerNumber = message.data["callerNumber"].orEmpty().take(64)
        val callerName = message.data["callerName"]
            ?.trim()
            ?.take(120)
            ?.takeIf(String::isNotBlank)
        SipForegroundService.start(applicationContext)
        SipForegroundService.showIncoming(
            applicationContext,
            IncomingSipCall(number = callerNumber, displayName = callerName),
            callId = callId
        )

        // A notificação controla quando a interface deve vir ao primeiro
        // plano. O registro SIP continua sendo restaurado silenciosamente.
        FirebaseMessaging.getInstance().register()
    }
}
