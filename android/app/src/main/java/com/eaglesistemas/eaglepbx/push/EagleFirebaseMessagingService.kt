package com.eaglesistemas.eaglepbx.push

import android.content.Intent
import com.eaglesistemas.eaglepbx.MainActivity
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
        if (eventType == "mobile_device_approved" || eventType == "mobile_device_revoked") {
            if (eventType == "mobile_device_revoked") {
                SipForegroundService.stop(applicationContext)
            }
            MobileProvisioningEvents.publish(
                type = eventType,
                deviceId = message.data["deviceId"].orEmpty().take(64)
            )
            return
        }
        val callId = message.data["callId"].orEmpty().take(160)
        if (eventType == "incoming_call_cancelled") {
            SipForegroundService.cancelIncoming(applicationContext, callId)
            return
        }
        if (eventType != "incoming_call") return
        val sessionStore = SecureSessionStore(applicationContext)
        if (sessionStore.read().isNullOrBlank()) return

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

        // The native screen owns the SIP engine today. Bringing it forward
        // lets the authenticated session restore and register before Asterisk
        // performs the delayed mobile attempt.
        runCatching {
            startActivity(
                Intent(applicationContext, MainActivity::class.java).apply {
                    action = SipForegroundService.ACTION_SHOW_INCOMING
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        }
        FirebaseMessaging.getInstance().register()
    }
}
