package com.eaglesistemas.eaglepbx.telephony

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomingFullScreenPolicyTest {
    @Test
    fun fullScreenIntentIsNotDuplicatedWhenAvailable() {
        assertFalse(shouldManuallyLaunchIncomingCall(true, true))
    }

    @Test
    fun lockedDeviceUsesManualFallbackWithoutPermission() {
        assertTrue(shouldManuallyLaunchIncomingCall(true, false))
    }

    @Test
    fun unlockedDeviceNeverUsesManualFallback() {
        assertFalse(shouldManuallyLaunchIncomingCall(false, false))
    }

    @Test
    fun fallbackLaunchesWhenLockedCallRemainsActiveAndActivityIsUnseen() {
        assertTrue(shouldRunIncomingCallFallback(true, true, false))
    }

    @Test
    fun fallbackDoesNotDuplicateVisibleIncomingActivity() {
        assertFalse(shouldRunIncomingCallFallback(true, true, true))
    }

    @Test
    fun fallbackIgnoresCallThatAlreadyEnded() {
        assertFalse(shouldRunIncomingCallFallback(true, false, false))
    }

    @Test
    fun pushAndSipForSameCallAreDeduplicatedByNumber() {
        assertTrue(isSameIncomingCall("104", "push-1", "104", ""))
    }

    @Test
    fun repeatedPushForSameCallIsDeduplicatedById() {
        assertTrue(isSameIncomingCall("104", "push-1", "104", "push-1"))
    }

    @Test
    fun differentCallIdsFromSameNumberRemainIndependent() {
        assertFalse(isSameIncomingCall("104", "push-1", "104", "push-2"))
    }

    @Test
    fun callAfterPreviousCycleEndedIsNeverDeduplicated() {
        assertFalse(isSameIncomingCall(null, "", "104", "push-2"))
    }

    @Test
    fun consecutiveSipCallsFromSameNumberRemainIndependent() {
        assertFalse(
            isSameIncomingCall(
                activeNumber = "104",
                activeCallId = "push-1",
                incomingNumber = "104",
                incomingCallId = "",
                activeSipCallId = "sip-call-1",
                incomingSipCallId = "sip-call-2"
            )
        )
    }

    @Test
    fun sipInviteBindsToItsEarlierPushByNumber() {
        assertTrue(
            isSameIncomingCall(
                activeNumber = "104",
                activeCallId = "push-2",
                incomingNumber = "104",
                incomingCallId = "",
                activeSipCallId = null,
                incomingSipCallId = "sip-call-2"
            )
        )
    }

    @Test
    fun previousOngoingCompletionDoesNotTargetNextIncomingCall() {
        assertEquals(
            SipCallCompletionTargets(incoming = false, ongoing = true),
            sipCallCompletionTargets(
                currentIncomingSipCallId = "sip-call-2",
                ongoingSipCallId = "sip-call-1",
                completedSipCallId = "sip-call-1"
            )
        )
    }

    @Test
    fun staleDuplicateCompletionTargetsNothing() {
        assertEquals(
            SipCallCompletionTargets(incoming = false, ongoing = false),
            sipCallCompletionTargets(
                currentIncomingSipCallId = "sip-call-2",
                ongoingSipCallId = null,
                completedSipCallId = "sip-call-1"
            )
        )
    }

    @Test
    fun lateTerminalEventDoesNotOwnTheNextActiveSipCall() {
        assertFalse(
            terminalEventOwnsActiveCall(
                activeCallKey = "sip-call-2",
                terminalCallKey = "sip-call-1"
            )
        )
    }

    @Test
    fun terminalEventClearsOnlyItsOwnActiveSipCall() {
        assertTrue(
            terminalEventOwnsActiveCall(
                activeCallKey = "sip-call-2",
                terminalCallKey = "sip-call-2"
            )
        )
    }
}
