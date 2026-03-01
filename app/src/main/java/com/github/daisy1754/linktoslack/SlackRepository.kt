package com.github.daisy1754.linktoslack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class SlackRepository {
    private val client = OkHttpClient()

    suspend fun openDmChannel(token: String, userId: String): String = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("users", userId)
            .build()
        val request = Request.Builder()
            .url("https://slack.com/api/conversations.open")
            .header("Authorization", "Bearer $token")
            .post(formBody)
            .build()
        val body = client.newCall(request).execute().use { it.body!!.string() }
        val json = JSONObject(body)
        check(json.getBoolean("ok")) { json.optString("error", "conversations.open failed") }
        json.getJSONObject("channel").getString("id")
    }

    suspend fun sendMessage(token: String, channelId: String, text: String) = withContext(Dispatchers.IO) {
        val formBody = FormBody.Builder()
            .add("channel", channelId)
            .add("text", text)
            .build()
        val request = Request.Builder()
            .url("https://slack.com/api/chat.postMessage")
            .header("Authorization", "Bearer $token")
            .post(formBody)
            .build()
        val body = client.newCall(request).execute().use { it.body!!.string() }
        val json = JSONObject(body)
        check(json.getBoolean("ok")) { json.optString("error", "chat.postMessage failed") }
    }
}
