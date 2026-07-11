package com.androidblunders.rakshak.gemma

import android.content.Context
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

/** Progress of the on-device model download. */
data class ModelDownloadStatus(
    val isModelAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
)

/**
 * Locates and (resumably) downloads the Gemma 4 `.litertlm` weights into app
 * private storage. Kept separate from inference so the model lifecycle can be
 * driven by an onboarding screen while the engine stays lazy.
 *
 * For the hackathon the file can also be side-loaded into
 * `filesDir/models/<MODEL_FILENAME>` to skip the ~2.7 GB download.
 */
@Singleton
class GemmaModelManager @Inject constructor(
    @param:ApplicationContext context: Context,
    private val httpClient: OkHttpClient,
) {
    private val modelDir = File(context.filesDir, "models").also { it.mkdirs() }
    private val modelFile = File(modelDir, MODEL_FILENAME)
    private val tempFile = File(modelDir, "$MODEL_FILENAME.tmp")

    private val _status = MutableStateFlow(
        ModelDownloadStatus(
            isModelAvailable = isModelAvailable(),
            downloadedBytes = currentSize(),
            totalBytes = MODEL_SIZE_BYTES,
        ),
    )
    val status: StateFlow<ModelDownloadStatus> = _status.asStateFlow()

    fun isModelAvailable(): Boolean = modelFile.exists() && modelFile.length() > 0L

    fun getModelPath(): String = modelFile.absolutePath

    private fun currentSize(): Long = if (modelFile.exists()) modelFile.length() else 0L

    suspend fun downloadModel(): Result<File> = withContext(Dispatchers.IO) {
        if (isModelAvailable()) {
            publish(isModelAvailable = true, downloadedBytes = modelFile.length(), progress = 1f)
            return@withContext Result.success(modelFile)
        }
        if (_status.value.isDownloading) {
            return@withContext Result.failure(IllegalStateException("Download already running."))
        }

        publish(isDownloading = true, progress = 0f)
        try {
            modelDir.mkdirs()
            val existing = if (tempFile.exists()) tempFile.length() else 0L
            val request = Request.Builder().url(MODEL_URL).apply {
                if (existing > 0L) addHeader("Range", "bytes=$existing-")
            }.build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != HTTP_PARTIAL_CONTENT) {
                    return@withContext Result.failure(
                        IllegalStateException("Download failed: HTTP ${response.code}"),
                    )
                }
                val body = response.body
                    ?: return@withContext Result.failure(IllegalStateException("Empty body"))

                val isPartial = response.code == HTTP_PARTIAL_CONTENT
                val total = when {
                    isPartial -> existing + body.contentLength()
                    body.contentLength() > 0L -> body.contentLength()
                    else -> MODEL_SIZE_BYTES
                }
                var downloaded = if (isPartial) existing else 0L
                val append = isPartial && existing > 0L

                body.byteStream().use { input ->
                    FileOutputStream(tempFile, append).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var read = input.read(buffer)
                        while (read != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            publish(
                                isDownloading = true,
                                downloadedBytes = downloaded,
                                totalBytes = total,
                                progress = if (total > 0) downloaded.toFloat() / total else 0f,
                            )
                            read = input.read(buffer)
                        }
                    }
                }
            }

            if (!tempFile.renameTo(modelFile)) {
                return@withContext Result.failure(IllegalStateException("Could not finalize model"))
            }
            publish(
                isModelAvailable = true, isDownloading = false,
                downloadedBytes = modelFile.length(), progress = 1f,
            )
            Result.success(modelFile)
        } catch (e: Exception) {
            publish(isDownloading = false)
            Result.failure(e)
        }
    }

    private fun publish(
        isModelAvailable: Boolean = _status.value.isModelAvailable,
        isDownloading: Boolean = _status.value.isDownloading,
        downloadedBytes: Long = _status.value.downloadedBytes,
        totalBytes: Long = _status.value.totalBytes,
        progress: Float = _status.value.progress,
    ) {
        _status.value = ModelDownloadStatus(
            isModelAvailable, isDownloading, downloadedBytes, totalBytes,
            progress.coerceIn(0f, 1f),
        )
    }

    companion object {
        private const val HTTP_PARTIAL_CONTENT = 206
        private const val BUFFER_SIZE = 65_536
        const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"
        private const val MODEL_SIZE_BYTES = 2_770_000_000L
        private const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    }
}
