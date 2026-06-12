package com.chatroom.app.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.chatroom.app.data.api.ApiService
import com.chatroom.app.data.model.FileInfo
import com.chatroom.app.data.model.UploadComplete
import java.io.File
import java.io.FileOutputStream

class FileRepository(
    private val api: ApiService,
    private val context: Context,
) {
    companion object {
        const val CHUNK_SIZE = 5 * 1024 * 1024 // 5 MB
    }

    /**
     * Upload a file from a content URI in chunks.
     * Calls onProgress with (bytesUploaded, totalBytes) for UI progress.
     */
    suspend fun uploadFile(
        uri: Uri,
        onProgress: (Long, Long) -> Unit,
    ): Result<UploadComplete> = runCatching {
        val contentResolver = context.contentResolver

        // Get file metadata
        var fileName = "file"
        var fileSize = 0L
        var mimeType = "application/octet-stream"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0) fileName = cursor.getString(nameIdx) ?: fileName
                if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
            }
        }

        mimeType = contentResolver.getType(uri) ?: mimeType

        val totalChunks = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()

        // 1. Init
        val init = api.initUpload(fileName, fileSize, mimeType, totalChunks, CHUNK_SIZE)
        if (!init.success) throw Exception(init.error ?: "上传初始化失败")

        val uploadId = init.uploadId!!
        val fileId = init.fileId!!

        // 2. Upload chunks
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw Exception("无法读取文件")

        inputStream.use { stream ->
            val buffer = ByteArray(CHUNK_SIZE)
            var chunkIndex = 0
            var uploaded = 0L

            while (chunkIndex < totalChunks) {
                val bytesRead = stream.read(buffer, 0, CHUNK_SIZE)
                if (bytesRead <= 0) break

                val chunk = if (bytesRead == CHUNK_SIZE) buffer else buffer.copyOf(bytesRead)

                // Retry up to 3 times
                var ok = false
                for (retry in 0..2) {
                    val resp = api.uploadChunk(uploadId, fileId, chunkIndex, chunk, fileName)
                    if (resp.success) { ok = true; break }
                    kotlinx.coroutines.delay(1000L * (retry + 1))
                }
                if (!ok) throw Exception("分块 $chunkIndex 上传失败")

                uploaded += bytesRead
                chunkIndex++
                onProgress(uploaded, fileSize)
            }
        }

        // 3. Complete
        val completeResult = api.completeUpload(uploadId, fileId)
        if (!completeResult.success) throw Exception(completeResult.error ?: "上传完成验证失败")

        completeResult
    }

    suspend fun getFileInfo(fileId: Int): Result<FileInfo> = runCatching {
        api.getFileInfo(fileId)
    }

    fun getDownloadUrl(fileId: Int): String = api.getDownloadUrl(fileId)

    /**
     * Download a file to the app's cache directory.
     * Calls onProgress with (bytesDownloaded, totalBytes).
     */
    suspend fun downloadFile(
        fileId: Int,
        fileName: String,
        fileSize: Long,
        onProgress: (Long, Long) -> Unit,
    ): Result<File> = runCatching {
        val url = api.getDownloadUrl(fileId)
        val client = okhttp3.OkHttpClient()
        val response = client.newCall(
            okhttp3.Request.Builder().url(url).build()
        ).execute()

        val body = response.body ?: throw Exception("响应体为空")

        val outputFile = File(context.cacheDir, fileName)
        FileOutputStream(outputFile).use { fos ->
            body.byteStream().use { input ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    fos.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    onProgress(downloaded, fileSize)
                }
            }
        }

        outputFile
    }
}
