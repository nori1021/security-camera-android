package com.securitycamera.app.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class FunctionsApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .callTimeout(330, TimeUnit.SECONDS)
        .build(),
) {

    suspend fun registerFace(
        baseUrl: String,
        functionKey: String,
        subjectId: String,
        jpegImages: List<ByteArray>,
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val imagesArr = org.json.JSONArray()
            for (b in jpegImages) {
                imagesArr.put(Base64.encodeToString(b, Base64.NO_WRAP))
            }
            val json = JSONObject().apply {
                put("subjectId", subjectId)
                put("images", imagesArr)
            }
            val url = buildUrl(baseUrl, "registerFace", functionKey)
            postJson(url, json).let { body ->
                JSONObject(body)
            }
        }
    }

    suspend fun analyze(
        baseUrl: String,
        functionKey: String,
        jpegImage: ByteArray,
        notifyEmail: String,
        sendAlertIfUnknown: Boolean = true,
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val json = JSONObject().apply {
                put("image", Base64.encodeToString(jpegImage, Base64.NO_WRAP))
                put("sendAlertIfUnknown", sendAlertIfUnknown)
                if (notifyEmail.isNotBlank()) put("notifyEmail", notifyEmail)
            }
            val url = buildUrl(baseUrl, "analyze", functionKey)
            postJson(url, json).let { JSONObject(it) }
        }
    }

    private fun buildUrl(base: String, route: String, code: String): String {
        val root = base.trimEnd('/')
        val enc = URLEncoder.encode(code, Charsets.UTF_8.name())
        return "$root/api/$route?code=$enc"
    }

    private fun postJson(url: String, json: JSONObject): String {
        val body = json.toString().toRequestBody(JSON_MEDIA)
        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val hint = buildString {
                    if (resp.code == 403 && text.contains("Face API", ignoreCase = true)) {
                        append(
                            " — Cloud functions may be an old deploy still calling Azure Face API; ",
                        )
                        append("redeploy from repo securitycam-functions (npm ci && func azure functionapp publish …).")
                    } else if (resp.code == 401) {
                        append(" — Check functions.key (host default key or per-function key).")
                    }
                }
                error("HTTP ${resp.code}: $text$hint")
            }
            return text
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        val SUBJECT_ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,128}$")

        fun validateSubjectId(id: String): Boolean = SUBJECT_ID_REGEX.matches(id.trim())
    }
}
