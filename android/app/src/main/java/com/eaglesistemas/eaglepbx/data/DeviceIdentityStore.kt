package com.eaglesistemas.eaglepbx.data

import android.content.Context
import java.util.UUID

class DeviceIdentityStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES,
        Context.MODE_PRIVATE
    )

    fun installationId(): String {
        preferences.getString(KEY_INSTALLATION_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { identifier ->
            preferences.edit()
                .putString(KEY_INSTALLATION_ID, identifier)
                .commit()
        }
    }

    private companion object {
        const val PREFERENCES = "eagle_pbx_device_identity"
        const val KEY_INSTALLATION_ID = "installation_id"
    }
}
