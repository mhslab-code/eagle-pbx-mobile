package com.eaglesistemas.eaglepbx.telephony

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
}
