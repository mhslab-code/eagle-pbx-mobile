package com.eaglesistemas.eaglepbx.data

import com.eaglesistemas.eaglepbx.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class EagleApiClient(
    private val sessionStore: SecureSessionStore
) {
    fun restoreSession(): AuthenticatedUser? {
        if (sessionStore.read().isNullOrBlank()) return null
        return try {
            requestUser("/api/me")
        } catch (error: ApiException) {
            if (error.statusCode == HttpURLConnection.HTTP_UNAUTHORIZED ||
                error.statusCode == HttpURLConnection.HTTP_FORBIDDEN
            ) {
                sessionStore.clear()
                null
            } else {
                throw error
            }
        }
    }

    fun login(extension: String, password: String): AuthenticatedUser {
        val body = "username=${encode(extension)}&password=${encode(password)}"
        val response = readResponse(
            connection("/api/auth/login", "POST").apply {
                setRequestProperty(
                    "Content-Type",
                    "application/x-www-form-urlencoded; charset=UTF-8"
                )
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        )
        val cookie = response.setCookies
            .firstOrNull { it.startsWith("$SESSION_COOKIE=", ignoreCase = true) }
            ?.substringBefore(';')
            ?: throw ApiException("O servidor não iniciou uma sessão segura.", response.status)

        val user = parseUser(JSONObject(response.body).getJSONObject("user"))
        if (user.role != "user" || user.extension.isBlank()) {
            bestEffortLogout(cookie)
            throw ApiException("Use uma conta de usuário vinculada a um ramal.", 403)
        }
        if (user.mustChangePassword) {
            bestEffortLogout(cookie)
            throw ApiException(
                "Conclua a troca obrigatória de senha antes de usar o aplicativo.",
                403
            )
        }
        sessionStore.save(cookie)
        return user
    }

    fun logout() {
        val cookie = sessionStore.read()
        if (!cookie.isNullOrBlank()) bestEffortLogout(cookie)
        sessionStore.clear()
    }

    fun updatePresence(presence: String): AuthenticatedUser {
        require(presence in setOf("online", "offline", "dnd"))
        val payload = JSONObject().put("presence", presence).toString()
        return requestUser("/api/me", "PATCH", payload)
    }

    private fun requestUser(path: String): AuthenticatedUser {
        return requestUser(path, "GET", null)
    }

    private fun requestUser(
        path: String,
        method: String,
        jsonBody: String?
    ): AuthenticatedUser {
        val response = readResponse(
            connection(path, method).apply {
                sessionStore.read()?.let { setRequestProperty("Cookie", it) }
                if (jsonBody != null) {
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                    outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                }
            }
        )
        return parseUser(JSONObject(response.body))
    }

    private fun bestEffortLogout(cookie: String) {
        runCatching {
            readResponse(
                connection("/api/auth/logout", "POST").apply {
                    setRequestProperty("Cookie", cookie)
                    doOutput = true
                    setFixedLengthStreamingMode(0)
                }
            )
        }
    }

    private fun connection(path: String, method: String): HttpsURLConnection {
        val baseUri = URI(BuildConfig.API_BASE_URL)
        require(baseUri.scheme == "https") { "A API deve usar HTTPS." }
        return baseUri.resolve(path).toURL().openConnection().let {
            require(it is HttpsURLConnection) { "Conexão HTTPS necessária." }
            it
        }.apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = false
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun readResponse(connection: HttpsURLConnection): ApiResponse {
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val setCookies = connection.headerFields.entries
                .firstOrNull { (name, _) -> name.equals("Set-Cookie", ignoreCase = true) }
                ?.value
                .orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(body).optString("detail") }
                    .getOrNull()
                    .takeUnless { it.isNullOrBlank() }
                    ?: "Não foi possível concluir a solicitação."
                throw ApiException(message, status)
            }
            return ApiResponse(status, body, setCookies)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseUser(json: JSONObject) = AuthenticatedUser(
        id = json.getLong("id"),
        username = json.getString("username"),
        name = json.getString("name"),
        email = json.optString("email"),
        extension = json.optString("extension"),
        avatar = json.optString("avatar").takeUnless { it.isBlank() || it == "null" },
        presence = json.optString("presence", "offline"),
        role = json.optString("role"),
        active = json.optBoolean("active"),
        mustChangePassword = json.optBoolean("mustChangePassword")
    )

    private fun encode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name())

    private data class ApiResponse(
        val status: Int,
        val body: String,
        val setCookies: List<String>
    )

    private companion object {
        const val SESSION_COOKIE = "eagle_session"
    }
}
