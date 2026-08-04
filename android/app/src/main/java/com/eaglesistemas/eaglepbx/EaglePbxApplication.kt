package com.eaglesistemas.eaglepbx

import android.app.Application
import com.eaglesistemas.eaglepbx.telephony.EagleTelecomController
import com.eaglesistemas.eaglepbx.telephony.SipForegroundService
import com.eaglesistemas.eaglepbx.ui.login.LoginViewModel

/**
 * Owns the telephony controller for the whole process. Incoming calls can start
 * from FCM, the foreground service or the lock-screen activity, so their SIP
 * lifecycle must not depend on MainActivity existing.
 */
class EaglePbxApplication : Application() {
    lateinit var loginViewModel: LoginViewModel
        private set
    var telecomController: EagleTelecomController? = null
        private set

    override fun onCreate() {
        super.onCreate()
        loginViewModel = LoginViewModel(this)
        telecomController = EagleTelecomController(
            context = this,
            answerSipCall = {
                loginViewModel.answerIncomingFromNotification(
                    SipForegroundService.currentIncomingCall()
                )
            },
            disconnectSipCall = { _ ->
                if (SipForegroundService.currentIncomingCall() != null) {
                    loginViewModel.rejectIncomingFromTelecom()
                } else {
                    loginViewModel.hangupCall()
                }
            }
        )
    }
}
