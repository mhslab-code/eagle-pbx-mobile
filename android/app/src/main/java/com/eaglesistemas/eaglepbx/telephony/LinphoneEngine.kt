package com.eaglesistemas.eaglepbx.telephony

import android.content.Context
import org.linphone.core.Core
import org.linphone.core.Factory

enum class SipEngineStatus {
    INITIALIZING,
    READY,
    UNAVAILABLE
}

/**
 * Owns the native SIP core without provisioning an account.
 *
 * SIP credentials will only be supplied by the authenticated per-device
 * provisioning flow in a later phase. They must never be embedded here.
 */
class LinphoneEngine(context: Context) {
    private val core: Core = Factory.instance().createCore(
        null,
        null,
        context.applicationContext
    )

    fun start() {
        core.start()
    }

    fun stop() {
        core.stop()
    }
}
