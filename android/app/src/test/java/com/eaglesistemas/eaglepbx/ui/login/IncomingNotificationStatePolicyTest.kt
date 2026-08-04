package com.eaglesistemas.eaglepbx.ui.login

import com.eaglesistemas.eaglepbx.telephony.SipCallStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingNotificationStatePolicyTest {
    @Test
    fun activeIncomingCallMayBePresented() {
        assertTrue(canPresentIncomingFromNotification(SipCallStatus.IDLE, true))
        assertTrue(canPresentIncomingFromNotification(SipCallStatus.INCOMING, true))
    }

    @Test
    fun staleNotificationCannotRevertAnsweredCall() {
        assertFalse(canPresentIncomingFromNotification(SipCallStatus.CONNECTED, true))
        assertFalse(canPresentIncomingFromNotification(SipCallStatus.HELD, true))
    }

    @Test
    fun notificationWithoutLiveServiceCallIsIgnored() {
        assertFalse(canPresentIncomingFromNotification(SipCallStatus.IDLE, false))
    }
}
