package com.eaglesistemas.eaglepbx.push

import com.eaglesistemas.eaglepbx.data.DeviceIdentityStore
import com.eaglesistemas.eaglepbx.data.EagleApiClient
import com.eaglesistemas.eaglepbx.data.SecureSessionStore
import com.google.firebase.messaging.FirebaseMessagingService

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
}
