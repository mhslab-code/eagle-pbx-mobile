package com.eaglesistemas.eaglepbx.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(cookie: String) {
        saveEncrypted(KEY_COOKIE, KEY_IV, cookie)
    }

    fun read(): String? {
        return readEncrypted(KEY_COOKIE, KEY_IV)
    }

    fun saveUser(user: AuthenticatedUser) {
        saveEncrypted(
            KEY_USER,
            KEY_USER_IV,
            JSONObject()
                .put("id", user.id)
                .put("username", user.username)
                .put("name", user.name)
                .put("email", user.email)
                .put("extension", user.extension)
                .put("avatar", user.avatar)
                .put("presence", user.presence)
                .put("role", user.role)
                .put("active", user.active)
                .put("mustChangePassword", user.mustChangePassword)
                .toString()
        )
    }

    fun readUser(): AuthenticatedUser? = readEncrypted(KEY_USER, KEY_USER_IV)?.let {
        runCatching {
            JSONObject(it).let { json ->
                AuthenticatedUser(
                    id = json.getLong("id"),
                    username = json.getString("username"),
                    name = json.getString("name"),
                    email = json.optString("email"),
                    extension = json.optString("extension"),
                    avatar = json.optString("avatar").takeUnless(String::isBlank),
                    presence = json.optString("presence", "online"),
                    role = json.optString("role", "user"),
                    active = json.optBoolean("active", true),
                    mustChangePassword = json.optBoolean("mustChangePassword")
                )
            }
        }.getOrNull()
    }

    fun saveContacts(extension: String, contacts: List<EagleContact>) {
        val payload = JSONObject()
            .put("extension", extension)
            .put("items", JSONArray().apply {
                contacts.forEach { contact ->
                    put(JSONObject()
                        .put("name", contact.name)
                        .put("photo", contact.photo)
                        .put("numbers", JSONArray().apply {
                            contact.numbers.forEach { entry ->
                                put(JSONObject()
                                    .put("number", entry.number)
                                    .put("label", entry.label))
                            }
                        }))
                }
            })
            .toString()
        saveEncrypted(KEY_CONTACTS, KEY_CONTACTS_IV, payload)
    }

    fun readContacts(extension: String): List<EagleContact>? {
        val encoded = readEncrypted(KEY_CONTACTS, KEY_CONTACTS_IV) ?: return null
        return runCatching {
            val payload = JSONObject(encoded)
            require(payload.optString("extension") == extension)
            val items = payload.optJSONArray("items") ?: JSONArray()
            val contacts = mutableListOf<EagleContact>()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val numbersJson = item.optJSONArray("numbers") ?: JSONArray()
                val numbers = mutableListOf<ContactNumber>()
                for (numberIndex in 0 until numbersJson.length()) {
                    val number = numbersJson.optJSONObject(numberIndex) ?: continue
                    numbers += ContactNumber(
                        number = number.optString("number"),
                        label = number.optString("label", "Telefone")
                    )
                }
                contacts += EagleContact(
                    name = item.optString("name", "Contato"),
                    photo = item.optString("photo")
                        .takeUnless { it.isBlank() || it == "null" },
                    numbers = numbers
                )
            }
            contacts
        }.getOrNull()
    }

    fun saveHistory(extension: String, calls: List<HistoryCall>) {
        val payload = JSONObject()
            .put("extension", extension)
            .put("items", JSONArray().apply {
                calls.take(MAX_CACHED_HISTORY).forEach { call ->
                    put(JSONObject()
                        .put("id", call.id)
                        .put("direction", call.direction)
                        .put("remoteNumber", call.remoteNumber)
                        .put("remoteName", call.remoteName)
                        .put("remoteAvatar", call.remoteAvatar)
                        .put("startedAt", call.startedAt)
                        .put("durationSeconds", call.durationSeconds)
                        .put("result", call.result)
                        .put("recording", call.recording))
                }
            })
            .toString()
        saveEncrypted(KEY_HISTORY, KEY_HISTORY_IV, payload)
    }

    fun readHistory(extension: String): List<HistoryCall>? {
        val encoded = readEncrypted(KEY_HISTORY, KEY_HISTORY_IV) ?: return null
        return runCatching {
            val payload = JSONObject(encoded)
            require(payload.optString("extension") == extension)
            val items = payload.optJSONArray("items") ?: JSONArray()
            val calls = mutableListOf<HistoryCall>()
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                calls += HistoryCall(
                    id = item.optString("id"),
                    direction = item.optString("direction"),
                    remoteNumber = item.optString("remoteNumber"),
                    remoteName = item.optString("remoteName"),
                    remoteAvatar = item.optString("remoteAvatar")
                        .takeUnless { it.isBlank() || it == "null" },
                    startedAt = item.optString("startedAt"),
                    durationSeconds = item.optInt("durationSeconds"),
                    result = item.optString("result"),
                    recording = item.optBoolean("recording")
                )
            }
            calls
        }.getOrNull()
    }

    fun saveSipProvisioning(provisioning: SipProvisioning) {
        saveEncrypted(
            KEY_SIP_PROVISIONING,
            KEY_SIP_PROVISIONING_IV,
            JSONObject()
                .put("username", provisioning.username)
                .put("password", provisioning.password)
                .put("domain", provisioning.domain)
                .put("port", provisioning.port)
                .put("transport", provisioning.transport)
                .toString()
        )
    }

    fun readSipProvisioning(): SipProvisioning? =
        readEncrypted(KEY_SIP_PROVISIONING, KEY_SIP_PROVISIONING_IV)?.let {
            runCatching {
                JSONObject(it).let { json ->
                    SipProvisioning(
                        username = json.getString("username"),
                        password = json.getString("password"),
                        domain = json.getString("domain"),
                        port = json.getInt("port"),
                        transport = json.getString("transport")
                    ).also { provisioning ->
                        require(provisioning.username.isNotBlank())
                        require(provisioning.password.isNotBlank())
                        require(provisioning.domain.isNotBlank())
                        require(provisioning.port in 1..65535)
                        require(provisioning.transport == "tls")
                    }
                }
            }.getOrNull()
        }

    fun clearSipProvisioning() {
        preferences.edit()
            .remove(KEY_SIP_PROVISIONING)
            .remove(KEY_SIP_PROVISIONING_IV)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun saveEncrypted(valueKey: String, ivKey: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(valueKey, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(ivKey, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun readEncrypted(valueKey: String, ivKey: String): String? {
        val encryptedValue = preferences.getString(valueKey, null) ?: return null
        val ivValue = preferences.getString(ivKey, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.decode(ivValue, Base64.NO_WRAP))
            )
            cipher.doFinal(Base64.decode(encryptedValue, Base64.NO_WRAP))
                .toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES = "eagle_pbx_secure_session"
        const val KEY_COOKIE = "cookie"
        const val KEY_IV = "iv"
        const val KEY_USER = "user"
        const val KEY_USER_IV = "user_iv"
        const val KEY_CONTACTS = "contacts"
        const val KEY_CONTACTS_IV = "contacts_iv"
        const val KEY_HISTORY = "history"
        const val KEY_HISTORY_IV = "history_iv"
        const val KEY_SIP_PROVISIONING = "sip_provisioning"
        const val KEY_SIP_PROVISIONING_IV = "sip_provisioning_iv"
        const val MAX_CACHED_HISTORY = 200
        const val KEY_ALIAS = "eagle_pbx_session_v1"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
