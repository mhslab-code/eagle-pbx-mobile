package com.eaglesistemas.eaglepbx.telephony

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingAlertModeTest {
    @Test
    fun normalModeUsesCorporateRingtone() {
        assertEquals(
            IncomingAlertMode.SOUND,
            incomingAlertMode(AudioManager.RINGER_MODE_NORMAL)
        )
    }

    @Test
    fun vibrateModeUsesNativeVibration() {
        assertEquals(
            IncomingAlertMode.VIBRATE,
            incomingAlertMode(AudioManager.RINGER_MODE_VIBRATE)
        )
    }

    @Test
    fun silentModeDoesNotAlert() {
        assertEquals(
            IncomingAlertMode.SILENT,
            incomingAlertMode(AudioManager.RINGER_MODE_SILENT)
        )
    }
}
