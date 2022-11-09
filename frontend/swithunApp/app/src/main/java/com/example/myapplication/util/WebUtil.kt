package com.example.myapplication.util

import android.annotation.SuppressLint
import android.util.Log
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await

val TAG = "swithun-xxxx 「WebUtil    」"

fun postRequest(my_url: String, params: Map<String, String>): JSONObject? {
    /** 创建HttpClient  */
    val httpClient = OkHttpClient.Builder().build()

    val formBody: RequestBody = FormBody.Builder().apply {
        for ((key, value) in params) {
            add(key, value)
        }
    }.build()

    val requestBody = Request.Builder()
        .url(my_url)
        .post(formBody)
        .build()

    val response = httpClient.newCall(requestBody).execute()

    return response.body?.string()?.let {
        JSONObject(it)
    }
}

@SuppressLint("LongLogTag")
suspend fun getRequest(
    my_url: String, params: Map<String, String>? = null,
    headerParam: Map<String, String>? = null
): JSONObject? {
    val response = getRequestWithOriginalResponse(my_url, params, headerParam) ?: return null
    return response.getRequestBodyJsonObject()
}

suspend fun getRequestWithOriginalResponse(
    my_url: String,
    queryParam: Map<String, String>? = null,
    headerParam: Map<String, String>? = null
): Response? {
    val httpClient = OkHttpClient.Builder().build()

    val httpBuilder: HttpUrl.Builder = my_url.toHttpUrlOrNull()?.newBuilder() ?: return null
    if (queryParam != null) {
        for ((key, value) in queryParam) {
            httpBuilder.addQueryParameter(key, value)
        }
    }

    val request = Request.Builder()
        .url(httpBuilder.build())
        .apply {
            if (headerParam != null) {
                val headers = Headers.Builder()
                for ((key, value) in headerParam) {
                    headers.add(key, value)
                }
                headers(headers.build())
            }
        }
        .build()
    return httpClient.newCall(request).await()

}

@SuppressLint("LongLogTag")
fun Response.getRequestBodyJsonObject(): JSONObject? {
    return this.body?.string()?.let {
        Log.d(TAG, it ?: "")
        JSONObject(it)
    }
}

@SuppressLint("LongLogTag")
fun Response.getRequestCookies(): List<String> {
    return this.headers.values("Set-Cookie").also {
        for (cookie in it) {
            Log.d(TAG, "cookie: $cookie")
        }
    }
}