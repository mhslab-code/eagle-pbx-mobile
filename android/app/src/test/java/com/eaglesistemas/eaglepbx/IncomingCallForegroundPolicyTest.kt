package com.eaglesistemas.eaglepbx

import com.eaglesistemas.eaglepbx.telephony.SipCallStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingCallForegroundPolicyTest {
    @Test
    fun transientIdleDoesNotMinimizeWhileServiceStillHasIncomingCall() {
        assertFalse(
            shouldReturnIncomingCallActivityToBackground(
                returnRequested = true,
                incomingCallObserved = true,
                callStatus = SipCallStatus.IDLE,
                uiIncomingCallActive = false,
                serviceIncomingCallActive = true
            )
        )
    }

    @Test
    fun transientIdleDoesNotMinimizeWhileModalStillHasIncomingCall() {
        assertFalse(
            shouldReturnIncomingCallActivityToBackground(
                returnRequested = true,
                incomingCallObserved = true,
                callStatus = SipCallStatus.IDLE,
                uiIncomingCallActive = true,
                serviceIncomingCallActive = false
            )
        )
    }

    @Test
    fun completedCallMayReturnToPreviousLockedState() {
        assertTrue(
            shouldReturnIncomingCallActivityToBackground(
                returnRequested = true,
                incomingCallObserved = true,
                callStatus = SipCallStatus.IDLE,
                uiIncomingCallActive = false,
                serviceIncomingCallActive = false
            )
        )
    }

    @Test
    fun ordinaryIdleNeverMinimizesApplication() {
        assertFalse(
            shouldReturnIncomingCallActivityToBackground(
                returnRequested = false,
                incomingCallObserved = true,
                callStatus = SipCallStatus.IDLE,
                uiIncomingCallActive = false,
                serviceIncomingCallActive = false
            )
        )
    }
}
