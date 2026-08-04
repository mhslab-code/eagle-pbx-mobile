package com.eaglesistemas.eaglepbx.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureSipProvisioningStoreTest {
    @Test
    fun provisioningSurvivesEncryptedRoundTripAndCanBeRevokedSeparately() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SecureSessionStore(context)
        val provisioning = SipProvisioning(
            username = "mob101-test",
            password = "test-only-password",
            domain = "pbx.example.test",
            port = 5061,
            transport = "tls"
        )

        try {
            store.saveSipProvisioning(provisioning)
            assertEquals(provisioning, store.readSipProvisioning())
            val encryptedPayload = context
                .getSharedPreferences("eagle_pbx_secure_session", 0)
                .getString("sip_provisioning", null)
            assertNotNull(encryptedPayload)
            assertFalse(encryptedPayload.orEmpty().contains(provisioning.password))

            store.clearSipProvisioning()
            assertNull(store.readSipProvisioning())
        } finally {
            store.clearSipProvisioning()
        }
    }
}
