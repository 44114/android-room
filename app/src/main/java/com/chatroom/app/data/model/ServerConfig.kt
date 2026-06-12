package com.chatroom.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Public server config returned by GET /config. Field names use @SerialName to match server JSON. */
@Serializable
data class ServerConfig(
    @SerialName("turnstile_site_key")
    val turnstileSiteKey: String = "",

    @SerialName("turnstile_required_for_mobile")
    val turnstileRequiredForMobile: Boolean = false,

    @SerialName("max_file_size")
    val maxFileSize: Long = 4L * 1024 * 1024 * 1024,

    @SerialName("chunk_size")
    val chunkSize: Int = 5 * 1024 * 1024,

    @SerialName("allowed_mimetypes")
    val allowedMimetypes: List<String> = emptyList(),
)
