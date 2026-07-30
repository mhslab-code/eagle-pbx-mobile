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

class ApiException(
    message: String,
    val statusCode: Int
) : Exception(message)
