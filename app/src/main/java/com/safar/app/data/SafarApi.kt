package com.safar.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.safar.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Client for Safar's Cloudflare Worker (see /cloudflare): photo storage and push delivery.
 * Every call carries the user's Firebase ID token, which the Worker verifies.
 */
class SafarApi(private val auth: FirebaseAuth, private val baseUrl: String = BuildConfig.SAFAR_API_URL) {

    class ApiException(val status: Int, message: String) : IOException(message)

    /** Stores the JPEG as the signed-in user's photo; the Worker saves its URL on the profile too. */
    suspend fun uploadAvatar(jpeg: ByteArray): String =
        JSONObject(call("PUT", "/avatar", jpeg, "image/jpeg")).getString("photoUrl")

    suspend fun deleteAvatar() {
        call("DELETE", "/avatar")
    }

    /**
     * Pushes outlive the screen that triggered them: they run here, not in a ViewModel, so
     * leaving the screen (or a slow network) never cancels or delays the user's action.
     */
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Asks the Worker to push a notification the user just wrote. Returns at once; never throws. */
    fun pushNotification(toUid: String, notificationId: String) =
        push(JSONObject().put("kind", "notification").put("to", toUid).put("id", notificationId))

    /** Asks the Worker to push a chat message the user just sent. Returns at once; never throws. */
    fun pushMessage(conversationId: String, messageId: String) =
        push(JSONObject().put("kind", "message").put("cid", conversationId).put("id", messageId))

    /** Runs [block] in the background scope, for work that must finish even if the screen closes. */
    fun inBackground(block: suspend () -> Unit) {
        background.launch { runCatching { block() }.onFailure { Log.w(TAG, "Background task failed", it) } }
    }

    private fun push(body: JSONObject) = inBackground {
        // Retry network failures and server errors; a 4xx means the Worker refused it, so stop.
        for (attempt in 0 until PUSH_ATTEMPTS) {
            try {
                call("POST", "/notify", body.toString().toByteArray(), "application/json")
                return@inBackground
            } catch (e: ApiException) {
                if (e.status in 400..499) { Log.w(TAG, "Push refused: ${e.status} ${e.message}"); return@inBackground }
                Log.w(TAG, "Push attempt ${attempt + 1} failed: ${e.message}")
            } catch (e: IOException) {
                Log.w(TAG, "Push attempt ${attempt + 1} failed: ${e.message}")
            }
            delay(1_500L shl attempt)
        }
    }

    private companion object {
        const val TAG = "SafarApi"
        const val PUSH_ATTEMPTS = 3
    }

    private suspend fun call(method: String, path: String, body: ByteArray? = null, type: String? = null): String {
        val user = auth.currentUser ?: throw ApiException(401, "Sign in required")
        val token = user.getIdToken(false).await().token ?: throw ApiException(401, "Sign in required")
        return withContext(Dispatchers.IO) {
            val conn = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", type ?: "application/octet-stream")
                    conn.setFixedLengthStreamingMode(body.size)
                    conn.outputStream.use { it.write(body) }
                }
                val status = conn.responseCode
                val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (status !in 200..299) {
                    val message = runCatching { JSONObject(text).getString("error") }.getOrNull() ?: "Request failed ($status)"
                    throw ApiException(status, message)
                }
                text
            } finally {
                conn.disconnect()
            }
        }
    }
}
