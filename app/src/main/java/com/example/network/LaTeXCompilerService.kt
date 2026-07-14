package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object LaTeXCompilerService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun compileOnline(texCode: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "document.tex",
                    texCode.toRequestBody("text/plain".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url("https://latexonline.cc/compile")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    Result.success(bytes)
                } else {
                    Result.failure(Exception("Empty PDF returned from the LaTeX compiler server."))
                }
            } else {
                // Try to read compile log
                val errorLog = response.body?.string() ?: "Unknown error"
                val briefError = errorLog.lines().take(20).joinToString("\n")
                Result.failure(Exception(briefError))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
