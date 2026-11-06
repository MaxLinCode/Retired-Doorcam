package com.feebami.retiredsentinel

import android.content.Context
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class TelegramAlertSender {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(2, TimeUnit.MINUTES)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(3, TimeUnit.MINUTES)
        .build()

    // Status callbacks run on the caller or OkHttp thread; UI callers must dispatch to main.
    fun sendVideoAlert(context: Context, videoFile: File, caption: String = "Visitor approaching",
                       status: (String) -> Unit = {}) {
        val config = configuration(context, status) ?: return
        if (!videoFile.isFile || videoFile.length() == 0L) {
            status("Telegram: video is missing or empty")
            return
        }
        if (videoFile.length() > 50L * 1024 * 1024) {
            status("Telegram: video exceeds the bot upload limit (50 MB)")
            return
        }
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", config.second)
            .addFormDataPart("caption", caption.take(1024))
            .addFormDataPart("video", videoFile.name, videoFile.asRequestBody("video/mp4".toMediaType()))
            .build()
        status("Telegram: uploading video…")
        execute(Request.Builder().url("https://api.telegram.org/bot${config.first}/sendVideo")
            .post(body).build(), status)
    }

    fun sendTestMessage(context: Context, status: (String) -> Unit) {
        val config = configuration(context, status) ?: return
        status("Telegram: sending test…")
        val body = FormBody.Builder().add("chat_id", config.second)
            .add("text", "Doorcam test: Telegram connection is working.").build()
        execute(Request.Builder().url("https://api.telegram.org/bot${config.first}/sendMessage")
            .post(body).build(), status)
    }

    private fun configuration(context: Context, status: (String) -> Unit): Pair<String, String>? {
        if (!AppSettings.telegramEnabled) {
            status("Telegram: alerts are switched off")
            return null
        }
        val token = AppSettings.getTelegramToken(context).trim()
        val chatId = AppSettings.getTelegramChatId(context).trim()
        if (!token.matches(Regex("[0-9]+:[A-Za-z0-9_-]+"))) {
            status("Telegram: enter the bot token from BotFather (without a URL or 'bot' prefix)")
            return null
        }
        if (chatId.isBlank()) {
            status("Telegram: enter your chat ID")
            return null
        }
        return token to chatId
    }

    private fun execute(request: Request, status: (String) -> Unit) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Exception messages can include the request URL and bot token; never log them.
                val reason = when (e) {
                    is java.net.UnknownHostException -> "cannot reach Telegram; check Wi-Fi/internet"
                    is javax.net.ssl.SSLException -> "secure connection failed; check phone date/time and network"
                    is java.io.InterruptedIOException -> "request timed out; check internet and retry"
                    else -> "network request failed; check internet and retry"
                }
                status("Telegram: $reason")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val json = runCatching { JSONObject(it.body.string()) }.getOrNull()
                    if (it.isSuccessful && json?.optBoolean("ok") == true) {
                        status("Telegram: sent successfully")
                    } else {
                        val code = json?.optInt("error_code", it.code) ?: it.code
                        val description = json?.optString("description").orEmpty().lowercase()
                        val reason = when {
                            code == 401 || code == 404 -> "bot token rejected; check the token from BotFather"
                            code == 403 -> "bot cannot message this chat; unblock it and tap Start in Telegram"
                            "chat not found" in description -> "chat not found; tap Start in your bot chat and check the chat ID"
                            code == 429 -> "rate limited; wait before retrying"
                            code == 413 -> "video is too large"
                            code == 400 -> "request rejected; check the chat ID and bot access (HTTP 400)"
                            else -> "server rejected the request (HTTP $code); retry later"
                        }
                        status("Telegram: $reason")
                    }
                }
            }
        })
    }
}
