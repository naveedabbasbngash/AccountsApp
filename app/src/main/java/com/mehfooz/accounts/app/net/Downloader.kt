package com.mehfooz.accounts.app.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

object Downloader {
    private val client = OkHttpClient()
    private const val TAG = "Sync"

    /**
     * GET download with query params, saves to [destFile].
     * Calls [onProgress] with 0f..1f (only if server provides Content-Length).
     * Throws IllegalStateException on non-2xx with detailed body + URL.
     */
    suspend fun downloadWithProgress(
        baseUrl: String,
        queryParams: Map<String, String>,
        destFile: File,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {

        val urlBuilder = baseUrl.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("Bad URL: $baseUrl")

        queryParams.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
        val url = urlBuilder.build()

        Log.d(TAG, "HTTP GET -> $url")

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val code = resp.code
            val msg  = resp.message
            Log.d(TAG, "HTTP $code $msg for $url")

            if (!resp.isSuccessful) {
                val bodyStr = resp.body?.string()?.take(4000) ?: ""
                Log.e(TAG, "Non-2xx. URL=$url\nBody=$bodyStr")
                throw IllegalStateException("HTTP $code\nURL=$url\nBody=$bodyStr")
            }

            val body = resp.body ?: error("Empty body")
            val total = body.contentLength().takeIf { it > 0 } ?: -1L

            destFile.outputStream().use { out ->
                body.byteStream().use { input ->
                    val buf = ByteArray(8 * 1024)
                    var read: Int
                    var bytes = 0L
                    while (input.read(buf).also { read = it } != -1) {
                        out.write(buf, 0, read)
                        bytes += read
                        if (total > 0) onProgress(bytes.toFloat() / total.toFloat())
                    }
                }
            }
            if (total <= 0) onProgress(1f)
            Log.d(TAG, "Saved to ${destFile.absolutePath} (size=${destFile.length()})")
        }
    }
}