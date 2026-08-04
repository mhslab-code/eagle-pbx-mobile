package com.eaglesistemas.eaglepbx

import android.app.Notification
import android.app.NotificationManager
import android.os.SystemClock
import android.telecom.DisconnectCause
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.eaglesistemas.eaglepbx.telephony.IncomingSipCall
import com.eaglesistemas.eaglepbx.telephony.SipForegroundService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomingCallLockScreenTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @After
    fun cleanUp() {
        (context.applicationContext as EaglePbxApplication)
            .telecomController
            ?.disconnect(DisconnectCause.LOCAL)
        SipForegroundService.cancelIncoming(context, showMissed = false)
        SipForegroundService.finishOngoingCall(context)
        device.wakeUp()
        device.pressHome()
    }

    @Test
    fun connectedCallUpdatesTheSameCallStyleNotification() {
        device.wakeUp()
        device.pressHome()
        SipForegroundService.start(context)
        SystemClock.sleep(750L)
        SipForegroundService.showIncoming(
            context,
            IncomingSipCall(number = "104", displayName = "Teste Eagle"),
            callId = "instrumented-ongoing"
        )
        SystemClock.sleep(500L)

        SipForegroundService.markAnswered(context)

        val manager = context.getSystemService(NotificationManager::class.java)
        val deadline = SystemClock.uptimeMillis() + 3_000L
        var notification: Notification? = null
        while (SystemClock.uptimeMillis() < deadline) {
            notification = manager.activeNotifications
                .firstOrNull { it.id == 102 }
                ?.notification
            if (notification?.extras?.getCharSequence(Notification.EXTRA_TEXT) ==
                "Chamada em andamento"
            ) break
            SystemClock.sleep(100L)
        }

        requireNotNull(notification) { "Ongoing CallStyle notification was not posted" }
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.category == Notification.CATEGORY_CALL)
        assertTrue(
            notification.extras.getCharSequence(Notification.EXTRA_TEXT) ==
                "Chamada em andamento"
        )
    }

    @Test
    fun answersFirstCallAndPresentsThreeConsecutiveLockedCalls() {
        SipForegroundService.start(context)
        SystemClock.sleep(750L)
        repeat(3) { index ->
            device.pressHome()
            device.sleep()
            SystemClock.sleep(750L)

            SipForegroundService.showIncoming(
                context,
                IncomingSipCall(number = "104", displayName = "Teste Eagle"),
                callId = "instrumented-call-$index"
            )

            assertTrue(
                "Incoming call screen was not shown on cycle ${index + 1}",
                device.wait(Until.hasObject(By.text("CHAMADA RECEBIDA")), 7_000L)
            )
            val answer = requireNotNull(
                device.wait(Until.findObject(By.text("Atender")), 2_000L)
            ) { "Answer button was not available on cycle ${index + 1}" }

            if (index == 0) {
                answer.click()
                assertTrue(
                    "Answer action did not reach the process telephony controller",
                    device.wait(Until.hasObject(By.text("Conectando...")), 2_000L)
                )
                (context.applicationContext as EaglePbxApplication)
                    .telecomController
                    ?.disconnect(DisconnectCause.LOCAL)
                SipForegroundService.cancelIncoming(context, showMissed = false)
                SystemClock.sleep(500L)
            } else {
                val reject = requireNotNull(
                    device.wait(Until.findObject(By.text("Recusar")), 2_000L)
                ) { "Reject button was not available on cycle ${index + 1}" }
                reject.click()
            }

            assertTrue(
                "Incoming call screen did not close on cycle ${index + 1}",
                device.wait(Until.gone(By.text("CHAMADA RECEBIDA")), 5_000L)
            )
        }
    }

    @Test
    fun lateReleaseFromFirstCallPreservesSecondIncomingCall() {
        SipForegroundService.start(context)
        SystemClock.sleep(750L)
        SipForegroundService.showIncoming(
            context,
            IncomingSipCall(
                number = "104",
                displayName = "Primeira chamada",
                sipCallId = "sip-call-1"
            ),
            callId = "push-call-1"
        )
        SipForegroundService.markAnswered(context)

        SipForegroundService.showIncoming(
            context,
            IncomingSipCall(number = "104", displayName = "Segunda chamada"),
            callId = "push-call-2"
        )
        SipForegroundService.showIncoming(
            context,
            IncomingSipCall(
                number = "104",
                displayName = "Segunda chamada",
                sipCallId = "sip-call-2"
            )
        )

        assertTrue(
            SipForegroundService.finishSipCall(context, "sip-call-1")
        )
        assertEquals(
            "sip-call-2",
            SipForegroundService.currentIncomingCall()?.sipCallId
        )
        assertFalse(
            SipForegroundService.finishSipCall(context, "sip-call-1")
        )
        assertEquals(
            "sip-call-2",
            SipForegroundService.currentIncomingCall()?.sipCallId
        )

        assertTrue(
            SipForegroundService.finishSipCall(context, "sip-call-2")
        )
        assertNull(SipForegroundService.currentIncomingCall())
    }
}
