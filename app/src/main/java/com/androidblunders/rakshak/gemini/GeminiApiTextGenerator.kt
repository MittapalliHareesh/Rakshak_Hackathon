package com.androidblunders.rakshak.gemini

import com.androidblunders.rakshak.core.contract.TextGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A fast, lightweight fallback TextGenerator that calls the Gemini API via standard
 * HTTP REST. This is used if the 2.7GB Gemma 4 local model fails to download or load.
 * 
 * NOTE: Replace the API key with a valid key for the hackathon.
 */
@Singleton
class GeminiApiTextGenerator @Inject constructor() : TextGenerator {

    // For hackathon purposes, hardcode or inject this safely. 
    // IMPORTANT: Make sure to provide a valid key!
    private val apiKey = "YOUR_GEMINI_API_KEY_HERE"
    private val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

    private val _isReady = MutableStateFlow(true)
    override val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _backend = MutableStateFlow("Cloud (Gemini API Fallback)")
    override val backend: StateFlow<String> = _backend.asStateFlow()

    override suspend fun prepare(): Result<Unit> {
        // No download required, API is always "ready" as long as there is internet
        _isReady.value = true
        return Result.success(Unit)
    }

    override suspend fun generate(prompt: String, systemInstruction: String?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(endpointUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", prompt))

                val contentsArray = JSONArray()
                contentsArray.put(JSONObject().put("parts", partsArray))

                val requestBody = JSONObject()
                requestBody.put("contents", contentsArray)

                if (systemInstruction != null) {
                    val sysParts = JSONArray().put(JSONObject().put("text", systemInstruction))
                    val sysInstruction = JSONObject().put("parts", sysParts)
                    requestBody.put("system_instruction", sysInstruction)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseStr)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text")
                            return@withContext Result.success(text)
                        }
                    }
                    Result.failure(Exception("Failed to parse Gemini response"))
                } else {
                    val errorStr = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Result.failure(Exception("HTTP Error $responseCode: $errorStr"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun generateStream(prompt: String, systemInstruction: String?): Flow<String> = flow {
        // For simplicity in the fallback, we just call the one-shot generate and emit the result.
        // True streaming would require chunked HTTP reading of the stream endpoint.
        val result = generate(prompt, systemInstruction)
        result.onSuccess { text ->
            emit(text)
        }.onFailure { e ->
            emit("Error: ${e.message}")
        }
    }
}
