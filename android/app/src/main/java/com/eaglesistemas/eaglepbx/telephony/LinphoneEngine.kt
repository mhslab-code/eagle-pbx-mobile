package com.eaglesistemas.eaglepbx.telephony

import android.content.Context
import com.eaglesistemas.eaglepbx.data.SipProvisioning
import org.linphone.core.Account
import org.linphone.core.AuthInfo
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType

enum class SipEngineStatus {
    INITIALIZING,
    READY,
    REGISTERING,
    REGISTERED,
    REGISTRATION_FAILED,
    UNAVAILABLE
}

/**
 * Owns the native SIP core without provisioning an account.
 *
 * SIP credentials will only be supplied by the authenticated per-device
 * provisioning flow in a later phase. They must never be embedded here.
 */
class LinphoneEngine(
    context: Context,
    private val onStatusChanged: (SipEngineStatus) -> Unit
) {
    private val core: Core = Factory.instance().createCore(
        null,
        null,
        context.applicationContext
    )
    private var account: Account? = null
    private var authInfo: AuthInfo? = null
    private val listener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState,
            message: String
        ) {
            onStatusChanged(
                when (state) {
                    RegistrationState.Ok -> SipEngineStatus.REGISTERED
                    RegistrationState.Progress,
                    RegistrationState.Refreshing -> SipEngineStatus.REGISTERING
                    RegistrationState.Failed -> SipEngineStatus.REGISTRATION_FAILED
                    RegistrationState.Cleared,
                    RegistrationState.None -> SipEngineStatus.READY
                }
            )
        }
    }

    fun start() {
        core.addListener(listener)
        core.start()
    }

    fun configure(provisioning: SipProvisioning) {
        require(provisioning.transport == "tls")
        clearAccount()
        val factory = Factory.instance()
        val identity = requireNotNull(
            factory.createAddress(
                "sip:${provisioning.username}@${provisioning.domain}"
            )
        )
        val server = requireNotNull(
            factory.createAddress(
                "sips:${provisioning.domain}:${provisioning.port};transport=tls"
            )
        )
        val credentials = factory.createAuthInfo(
            provisioning.username,
            null,
            provisioning.password,
            null,
            null,
            provisioning.domain
        )
        val params = core.createAccountParams().apply {
            identityAddress = identity
            serverAddress = server
            transport = TransportType.Tls
            isRegisterEnabled = true
            expires = 600
        }
        core.mediaEncryption = MediaEncryption.SRTP
        core.isMediaEncryptionMandatory = true
        val configuredAccount = core.createAccount(params)
        core.addAuthInfo(credentials)
        core.addAccount(configuredAccount)
        core.defaultAccount = configuredAccount
        authInfo = credentials
        account = configuredAccount
        onStatusChanged(SipEngineStatus.REGISTERING)
    }

    fun clearAccount() {
        account?.let { core.removeAccount(it) }
        authInfo?.let { core.removeAuthInfo(it) }
        account = null
        authInfo = null
    }

    fun stop() {
        clearAccount()
        core.removeListener(listener)
        core.stop()
    }
}
