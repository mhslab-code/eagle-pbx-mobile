package com.eaglesistemas.eaglepbx.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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
        const val KEY_ALIAS = "eagle_pbx_session_v1"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
