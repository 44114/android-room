package com.chatroom.app.data.model

import kotlinx.serialization.Serializable

/** Generic API response from the server. */
@Serializable
data class ApiResponse(
    val success: Boolean? = null,
    val error: String? = null,
    val message: String? = null,
    val redirect: String? = null,
)

/** Response from /auth/check */
@Serializable
data class AuthStatus(
    val loggedIn: Boolean,
    val username: String? = null,
    val userId: Int? = null,
)

/** Response from /files/upload/init */
@Serializable
data class UploadInit(
    val success: Boolean = false,
    val fileId: Int? = null,
    val uploadId: String? = null,
    val chunkSize: Int = 0,
    val error: String? = null,
)

/** Response from /files/upload/chunk */
@Serializable
data class ChunkResponse(
    val success: Boolean = false,
    val chunkIndex: Int = 0,
    val receivedSize: Long = 0,
    val error: String? = null,
)

/** Response from /files/upload/complete */
@Serializable
data class UploadComplete(
    val success: Boolean = false,
    val fileId: Int? = null,
    val filename: String? = null,
    val fileSize: Long = 0,
    val error: String? = null,
)

/** Response from /files/info/<id> */
@Serializable
data class FileInfo(
    val id: Int = 0,
    val filename: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "application/octet-stream",
    val uploadComplete: Boolean = false,
    val createdAt: String? = null,
)
