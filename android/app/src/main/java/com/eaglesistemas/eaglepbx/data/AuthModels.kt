package com.eaglesistemas.eaglepbx.data

data class AuthenticatedUser(
    val id: Long,
    val username: String,
    val name: String,
    val email: String,
    val extension: String,
    val avatar: String?,
    val presence: String,
    val role: String,
    val active: Boolean,
    val mustChangePassword: Boolean
)

data class MobileDeviceRegistration(
    val id: String,
    val status: String,
    val reason: String,
    val deviceName: String
)

data class SipProvisioning(
    val username: String,
    val password: String,
    val domain: String,
    val port: Int,
    val transport: String
)

class ApiException(
    message: String,
    val statusCode: Int
) : Exception(message)
