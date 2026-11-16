package com.safar.app.data

import com.google.firebase.auth.FirebaseAuth
import com.safar.app.BuildConfig
import kotlinx.coroutines.Dispatchers
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

    /** Asks the Worker to push a notification the user just wrote. Best effort: never throws. */
    suspend fun pushNotification(toUid: String, notificationId: String) =
        push(JSONObject().put("kind", "notification").put("to", toUid).put("id", notificationId))

    /** Asks the Worker to push a chat message the user just sent. Best effort: never throws. */
    suspend fun pushMessage(conversationId: String, messageId: String) =
        push(JSONObject().put("kind", "message").put("cid", conversationId).put("id", messageId))

    private suspend fun push(body: JSONObject) {
        runCatching { call("POST", "/notify", body.toString().toByteArray(), "application/json") }
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
