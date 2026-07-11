package com.shubham.ondevicerag.feature.chat.data.ai

import android.content.Context
import com.shubham.ondevicerag.feature.chat.domain.model.ModelDownloadStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class GemmaModelManager @Inject constructor(
    @ApplicationContext context: Context,
    private val httpClient: OkHttpClient
) {
    private val modelDir = File(context.filesDir, "models").also { it.mkdirs() }
    private val modelFile = File(modelDir, MODEL_FILENAME)
    private val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")

    private val _modelStatus = MutableStateFlow(
        ModelDownloadStatus(
            isModelAvailable = isModelAvailable(),
            downloadedBytes = getModelSizeOnDisk(),
            totalBytes = MODEL_SIZE_BYTES
        )
    )
    val modelStatus: StateFlow<ModelDownloadStatus> = _modelStatus.asStateFlow()

    fun isModelAvailable(): Boolean = modelFile.exists() && modelFile.length() > 0L

    fun getModelPath(): String = modelFile.absolutePath

    private fun getModelSizeOnDisk(): Long = if (modelFile.exists()) modelFile.length() else 0L

    suspend fun downloadModel(): Result<File> = withContext(Dispatchers.IO) {
        if (isModelAvailable()) {
            publishStatus(isModelAvailable = true, downloadedBytes = modelFile.length(), progress = 1f)
            return@withContext Result.success(modelFile)
        }

        if (_modelStatus.value.isDownloading) {
            return@withContext Result.failure(IllegalStateException("Model download is already running."))
        }

        publishStatus(isDownloading = true, progress = 0f)

        try {
            modelDir.mkdirs()
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L
            val requestBuilder = Request.Builder().url(MODEL_URL)

            if (existingBytes > 0L) {
                requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != HTTP_PARTIAL_CONTENT) {
                    return@withContext Result.failure(
                        IllegalStateException("Download failed with HTTP ${response.code}.")
                    )
                }

                val body = response.body ?: return@withContext Result.failure(
                    IllegalStateException("Download failed because the response was empty.")
                )

                val contentLength = body.contentLength()
                val totalBytes = when {
                    response.code == HTTP_PARTIAL_CONTENT -> existingBytes + contentLength
                    contentLength > 0L -> contentLength
                    else -> MODEL_SIZE_BYTES
                }

                var downloadedBytes = if (response.code == HTTP_PARTIAL_CONTENT) existingBytes else 0L
                val append = response.code == HTTP_PARTIAL_CONTENT && existingBytes > 0L
                publishStatus(
                    isDownloading = true,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    progress = progress(downloadedBytes, totalBytes)
                )

                body.byteStream().use { input ->
                    FileOutputStream(tempFile, append).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read = input.read(buffer)

                        while (read != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            publishStatus(
                                isDownloading = true,
                                downloadedBytes = downloadedBytes,
                                totalBytes = totalBytes,
                                progress = progress(downloadedBytes, totalBytes)
                            )
                            read = input.read(buffer)
                        }
                    }
                }
            }

            if (!tempFile.renameTo(modelFile)) {
                return@withContext Result.failure(
                    IllegalStateException("Downloaded model could not be finalized.")
                )
            }

            publishStatus(
                isModelAvailable = true,
                isDownloading = false,
                downloadedBytes = modelFile.length(),
                progress = 1f
            )
            Result.success(modelFile)
        } catch (exception: Exception) {
            publishStatus(isDownloading = false)
            Result.failure(exception)
        }
    }

    private fun publishStatus(
        isModelAvailable: Boolean = _modelStatus.value.isModelAvailable,
        isDownloading: Boolean = _modelStatus.value.isDownloading,
        downloadedBytes: Long = _modelStatus.value.downloadedBytes,
        totalBytes: Long = _modelStatus.value.totalBytes,
        progress: Float = _modelStatus.value.progress
    ) {
        _modelStatus.value = ModelDownloadStatus(
            isModelAvailable = isModelAvailable,
            isDownloading = isDownloading,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes,
            progress = progress.coerceIn(0f, 1f)
        )
    }

    private fun progress(downloadedBytes: Long, totalBytes: Long): Float {
        if (totalBytes <= 0L) return 0f
        return downloadedBytes.toFloat() / totalBytes.toFloat()
    }

    companion object {
        private const val HTTP_PARTIAL_CONTENT = 206
        private const val BUFFER_SIZE = 65_536
        private const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"
        private const val MODEL_SIZE_BYTES = 2_770_000_000L
        private const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    }
}
