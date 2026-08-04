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
}
