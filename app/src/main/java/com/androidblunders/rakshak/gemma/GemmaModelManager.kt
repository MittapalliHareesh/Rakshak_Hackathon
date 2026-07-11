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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    /** Non-null when the last download attempt failed (network, gated model, etc.). */
    val error: String? = null,
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

    // Serializes download attempts — the dashboard button and the analyzer's
    // lazy prepare() can both call this; the second waits and then finds the
    // model already present instead of starting a second download.
    private val downloadMutex = Mutex()

    suspend fun downloadModel(): Result<File> = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            if (isModelAvailable()) {
                publish(isModelAvailable = true, isDownloading = false,
                    downloadedBytes = modelFile.length(), totalBytes = modelFile.length(),
                    progress = 1f, error = null)
                return@withLock Result.success(modelFile)
            }

            publish(isDownloading = true, progress = 0f, error = null)
            try {
                modelDir.mkdirs()
                val existing = if (tempFile.exists()) tempFile.length() else 0L
                val request = Request.Builder().url(MODEL_URL).apply {
                    if (existing > 0L) addHeader("Range", "bytes=$existing-")
                }.build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != HTTP_PARTIAL_CONTENT) {
                        return@withLock fail("HTTP ${response.code} — model may be gated; " +
                            "accept the license on Hugging Face or side-load the file")
                    }
                    val body = response.body ?: return@withLock fail("Empty response body")

                    // Guard against gated/redirect HTML pages being saved as "the model".
                    val contentType = body.contentType()?.toString().orEmpty()
                    if (contentType.contains("text/html", ignoreCase = true)) {
                        tempFile.delete()
                        return@withLock fail("Server returned a web page, not the model " +
                            "(login/gating). Side-load $MODEL_FILENAME instead")
                    }

                    val isPartial = response.code == HTTP_PARTIAL_CONTENT
                    val reported = body.contentLength()
                    val total = when {
                        isPartial && reported > 0 -> existing + reported
                        reported > 0L -> reported
                        else -> MODEL_SIZE_BYTES // indeterminate → fall back to estimate
                    }
                    var downloaded = if (isPartial) existing else 0L
                    val append = isPartial && existing > 0L

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile, append).use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read = input.read(buffer)
                            var lastPublished = 0L
                            while (read != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                // Throttle UI updates to ~every 1 MB to avoid flooding.
                                if (downloaded - lastPublished >= PROGRESS_STEP_BYTES) {
                                    lastPublished = downloaded
                                    publish(
                                        isDownloading = true,
                                        downloadedBytes = downloaded,
                                        totalBytes = total,
                                        progress = if (total > 0) downloaded.toFloat() / total else 0f,
                                    )
                                }
                                read = input.read(buffer)
                            }
                        }
                    }

                    // Reject an implausibly small file (partial/garbage) before finalizing.
                    if (tempFile.length() < MIN_VALID_BYTES) {
                        tempFile.delete()
                        return@withLock fail("Downloaded file too small " +
                            "(${tempFile.length() / 1_000_000} MB) — likely an error page")
                    }
                }

                if (!tempFile.renameTo(modelFile)) {
                    return@withLock fail("Could not finalize downloaded model")
                }
                publish(
                    isModelAvailable = true, isDownloading = false,
                    downloadedBytes = modelFile.length(), totalBytes = modelFile.length(),
                    progress = 1f, error = null,
                )
                Result.success(modelFile)
            } catch (e: Exception) {
                fail(e.message ?: "download error")
            }
        }
    }

    private fun fail(message: String): Result<File> {
        publish(isDownloading = false, error = message)
        return Result.failure(IllegalStateException(message))
    }

    private fun publish(
        isModelAvailable: Boolean = _status.value.isModelAvailable,
        isDownloading: Boolean = _status.value.isDownloading,
        downloadedBytes: Long = _status.value.downloadedBytes,
        totalBytes: Long = _status.value.totalBytes,
        progress: Float = _status.value.progress,
        error: String? = _status.value.error,
    ) {
        _status.value = ModelDownloadStatus(
            isModelAvailable, isDownloading, downloadedBytes, totalBytes,
            progress.coerceIn(0f, 1f), error,
        )
    }

    companion object {
        private const val HTTP_PARTIAL_CONTENT = 206
        private const val BUFFER_SIZE = 65_536
        private const val PROGRESS_STEP_BYTES = 1_000_000L // publish ~every 1 MB
        private const val MIN_VALID_BYTES = 100_000_000L   // reject error pages / partials
        const val MODEL_FILENAME = "gemma-4-E2B-it.litertlm"
        private const val MODEL_SIZE_BYTES = 2_770_000_000L
        private const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"
    }
}
