package com.chatroom.app.data.model

import kotlinx.serialization.SerialName
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
    @SerialName("logged_in")
    val loggedIn: Boolean,
    val username: String? = null,
    @SerialName("user_id")
    val userId: Int? = null,
)

/** Response from /files/upload/init */
@Serializable
data class UploadInit(
    val success: Boolean = false,
    @SerialName("file_id")
    val fileId: Int? = null,
    @SerialName("upload_id")
    val uploadId: String? = null,
    @SerialName("chunk_size")
    val chunkSize: Int = 0,
    val error: String? = null,
)

/** Response from /files/upload/chunk */
@Serializable
data class ChunkResponse(
    val success: Boolean = false,
    @SerialName("chunk_index")
    val chunkIndex: Int = 0,
    @SerialName("received_size")
    val receivedSize: Long = 0,
    val error: String? = null,
)

/** Response from /files/upload/complete */
@Serializable
data class UploadComplete(
    val success: Boolean = false,
    @SerialName("file_id")
    val fileId: Int? = null,
    val filename: String? = null,
    @SerialName("file_size")
    val fileSize: Long = 0,
    val error: String? = null,
)

/** Response from /files/info/<id> */
@Serializable
data class FileInfo(
    val id: Int = 0,
    val filename: String = "",
    @SerialName("file_size")
    val fileSize: Long = 0,
    @SerialName("mime_type")
    val mimeType: String = "application/octet-stream",
    @SerialName("upload_complete")
    val uploadComplete: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null,
)
