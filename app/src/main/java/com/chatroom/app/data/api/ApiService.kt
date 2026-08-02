package com.chatroom.app.data.api

import android.util.Log
import com.chatroom.app.BuildConfig
import com.chatroom.app.data.local.PreferencesManager
import com.chatroom.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HTTP client for all REST API calls.
 *
 * Uses OkHttp with automatic cookie management so session cookies
 * from login/register are persisted and sent with every request.
 *
 * Cookies and the CSRF token are mirrored to PreferencesManager
 * (DataStore) so an authenticated session survives process death:
 * on startup they are loaded back into the in-memory cookie jar and
 * the WebSocket handshake can authenticate without a fresh login.
 *
 * The server URL is read from PreferencesManager so the user can
 * configure it at runtime.
 */
class ApiService(private val prefs: PreferencesManager) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val cookieStoreSerializer = MapSerializer(
        String.serializer(),
        ListSerializer(PersistedCookie.serializer()),
    )

    /** Get current server URL — defaults to the build-time value if not configured. */
    val baseUrl: String
        get() = runBlocking { prefs.getServerUrlOnce() }
            .ifBlank { BuildConfig.BASE_URL }

    // Simple in-memory cookie jar — stores cookies by host.
    // (JavaNetCookieJar was removed from OkHttp 4.12.)
    private val cookieStore = java.util.concurrent.ConcurrentHashMap<String, MutableList<Cookie>>()

    private var cookiesLoaded = false

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore.getOrPut(url.host) { mutableListOf() }
                    .apply { cookies.forEach { c -> removeIf { it.name == c.name }; add(c) } }
                persistAuthState()
            }
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                loadPersistedCookies()
                return cookieStore[url.host]?.toList() ?: emptyList()
            }
        })
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("X-CSRF-Token", csrfToken)
                .addHeader("X-Client-Type", "android")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(HttpLoggingInterceptor { msg ->
            Log.d("ApiService", msg)
        }.apply { level = HttpLoggingInterceptor.Level.BASIC })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    /** CSRF token, updated from server responses. */
    var csrfToken: String = ""

    /** Exposed for FileRepository and SocketManager to use the same authenticated client. */
    fun getClient(): OkHttpClient = client

    /** Get session cookies for the current host (used by SocketManager for WebSocket handshake). */
    fun getCookiesForHost(): String {
        loadPersistedCookies()
        val host = runCatching { java.net.URI(baseUrl).host }.getOrNull() ?: return ""
        return cookieStore[host]?.joinToString("; ") { "${it.name}=${it.value}" } ?: ""
    }

    /** Drop all cookies and the CSRF token (called on logout / account deletion). */
    fun clearCookies() {
        cookieStore.clear()
        csrfToken = ""
        cookiesLoaded = true
        runBlocking {
            prefs.setCookies("")
            prefs.setCsrfToken("")
        }
    }

    /** Restore persisted cookies + CSRF token from DataStore into memory (once per process). */
    private fun loadPersistedCookies() {
        if (cookiesLoaded) return
        synchronized(this) {
            if (cookiesLoaded) return
            cookiesLoaded = true
            runBlocking {
                prefs.getCookiesOnce().takeIf { it.isNotBlank() }?.let { stored ->
                    val decoded = runCatching {
                        json.decodeFromString(cookieStoreSerializer, stored)
                    }.getOrNull() ?: emptyMap()
                    decoded.forEach { (host, cookies) ->
                        cookieStore[host] = cookies.map { it.toOkHttpCookie() }.toMutableList()
                    }
                }
                prefs.getCsrfTokenOnce().takeIf { it.isNotBlank() }?.let { csrfToken = it }
            }
        }
    }

    /** Write the current in-memory cookies + CSRF token to DataStore. */
    private fun persistAuthState() {
        runBlocking {
            val store: Map<String, List<PersistedCookie>> = cookieStore.mapValues { (_, list) ->
                list.map { it.toPersisted() }
            }
            prefs.setCookies(json.encodeToString(cookieStoreSerializer, store))
            if (csrfToken.isNotBlank()) prefs.setCsrfToken(csrfToken)
        }
    }

    private fun Cookie.toPersisted() = PersistedCookie(
        name = name,
        value = value,
        domain = domain,
        path = path,
        expiresAt = expiresAt,
        httpOnly = httpOnly,
        hostOnly = hostOnly,
        secure = secure,
    )

    private fun PersistedCookie.toOkHttpCookie(): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .path(path)
            .expiresAt(expiresAt)
            .apply {
                if (httpOnly) httpOnly()
                if (secure) secure()
            }
        return if (hostOnly) builder.hostOnlyDomain(domain).build()
        else builder.domain(domain).build()
    }

    // ── Auth ──────────────────────────────────────────────────────

    suspend fun register(
        username: String, password: String, passwordConfirm: String,
        inviteCode: String, turnstileToken: String,
    ): ApiResponse {
        val body = postBody(mapOf(
            "username" to username, "password" to password,
            "password_confirm" to passwordConfirm, "invite_code" to inviteCode,
            "cf-turnstile-response" to turnstileToken,
        ))
        return json.decodeFromString(post("$baseUrl/auth/register", body))
    }

    suspend fun login(
        username: String, password: String,
        rememberMe: Boolean = false, turnstileToken: String,
    ): ApiResponse {
        val body = postBody(mapOf(
            "username" to username, "password" to password,
            "remember_me" to if (rememberMe) "1" else "0",
            "cf-turnstile-response" to turnstileToken,
        ))
        return json.decodeFromString(post("$baseUrl/auth/login", body))
    }

    suspend fun logout(): ApiResponse {
        val body = postBody(emptyMap())
        return json.decodeFromString(post("$baseUrl/auth/logout", body))
    }

    suspend fun changePassword(
        currentPassword: String, newPassword: String, newPasswordConfirm: String,
    ): ApiResponse {
        val body = postBody(mapOf(
            "current_password" to currentPassword,
            "new_password" to newPassword,
            "new_password_confirm" to newPasswordConfirm,
        ))
        return json.decodeFromString(post("$baseUrl/auth/change-password", body))
    }

    suspend fun deleteAccount(password: String): ApiResponse {
        val body = postBody(mapOf("password" to password))
        return json.decodeFromString(post("$baseUrl/auth/delete-account", body))
    }

    suspend fun checkAuth(): AuthStatus {
        val resp = get("$baseUrl/auth/check")
        return json.decodeFromString(resp)
    }

    suspend fun fetchConfig(): ServerConfig {
        val resp = get("$baseUrl/config")
        return json.decodeFromString(resp)
    }

    // ── Files ─────────────────────────────────────────────────────

    suspend fun initUpload(
        filename: String, fileSize: Long, mimeType: String,
        totalChunks: Int, chunkSize: Int,
    ): UploadInit {
        val body = postBody(mapOf(
            "filename" to filename, "file_size" to fileSize.toString(),
            "mime_type" to mimeType, "total_chunks" to totalChunks.toString(),
            "chunk_size" to chunkSize.toString(),
        ))
        return json.decodeFromString(post("$baseUrl/files/upload/init", body))
    }

    suspend fun uploadChunk(
        uploadId: String, fileId: Int, chunkIndex: Int,
        chunkData: ByteArray, fileName: String,
    ): ChunkResponse = withContext(Dispatchers.IO) {
        val mpBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_id", uploadId)
            .addFormDataPart("file_id", fileId.toString())
            .addFormDataPart("chunk_index", chunkIndex.toString())
            .addFormDataPart("chunk", "$fileName.part$chunkIndex",
                chunkData.toRequestBody("application/octet-stream".toMediaType()))
            .build()

        val req = Request.Builder()
            .url("$baseUrl/files/upload/chunk")
            .post(mpBody)
            .build()

        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: "{}"
        json.decodeFromString(respBody)
    }

    suspend fun completeUpload(uploadId: String, fileId: Int): UploadComplete {
        val body = postBody(mapOf("upload_id" to uploadId, "file_id" to fileId.toString()))
        return json.decodeFromString(post("$baseUrl/files/upload/complete", body))
    }

    suspend fun getFileInfo(fileId: Int): FileInfo {
        val resp = get("$baseUrl/files/info/$fileId")
        return json.decodeFromString(resp)
    }

    fun getDownloadUrl(fileId: Int): String = "$baseUrl/files/download/$fileId"

    // ── Internal helpers ──────────────────────────────────────────

    /** Build a JSON request body from a Map. Uses org.json to avoid serialization boilerplate. */
    private fun postBody(params: Map<String, String>): RequestBody {
        val obj = JSONObject()
        params.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString().toRequestBody(jsonMediaType)
    }

    private suspend fun post(url: String, body: RequestBody): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(url)
            .post(body)
            .build()
        val resp = client.newCall(req).execute()
        val respBody = resp.body?.string() ?: "{}"
        resp.header("X-CSRF-Token")?.let { token ->
            if (token != csrfToken) {
                csrfToken = token
                persistAuthState()
            }
        }
        respBody
    }

    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).get().build()
        val resp = client.newCall(req).execute()
        resp.header("X-CSRF-Token")?.let { token ->
            if (token != csrfToken) {
                csrfToken = token
                persistAuthState()
            }
        }
        resp.body?.string() ?: "{}"
    }
}

/** Serializable cookie snapshot for persisting the OkHttp cookie jar in DataStore. */
@Serializable
data class PersistedCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
    val secure: Boolean,
)
