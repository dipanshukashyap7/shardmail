package com.shardmail.app.data.remote

import com.shardmail.app.data.local.SecurePreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkClient(private val securePreferences: SecurePreferences) {

    @Volatile
    private var cachedApi: ShardMailApi? = null
    @Volatile
    private var lastUrl: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = securePreferences.adminPassword.trim()
        val requestBuilder = original.newBuilder()
            .header("Content-Type", "application/json")
        if (token.isNotBlank()) {
            requestBuilder.header("x-auth-token", token)
        }
        chain.proceed(requestBuilder.build())
    }

    private val retryInterceptor = Interceptor { chain ->
        val request = chain.request()
        var response: Response? = null
        var lastException: IOException? = null
        val maxAttempts = 3
        val delaysMs = listOf(1000L, 2000L, 2000L)

        for (attempt in 0 until maxAttempts) {
            try {
                response?.close()
                response = chain.proceed(request)
                if (response.isSuccessful || response.code < 500) {
                    return@Interceptor response
                }
            } catch (e: IOException) {
                lastException = e
            }

            if (attempt < maxAttempts - 1) {
                try {
                    Thread.sleep(delaysMs[attempt])
                } catch (ignored: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }

        if (response != null) {
            return@Interceptor response
        }
        throw lastException ?: IOException("Worker not reachable — check the URL / deploy status")
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(retryInterceptor)
        .build()

    fun invalidate() {
        synchronized(this) {
            cachedApi = null
            lastUrl = null
        }
    }

    fun createApi(): ShardMailApi? {
        if (!securePreferences.isWorkerUrlValid()) {
            return null
        }

        val baseUrl = securePreferences.workerUrl.trim()
        val formattedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val currentApi = cachedApi
        if (currentApi != null && lastUrl == formattedUrl) {
            return currentApi
        }

        return synchronized(this) {
            if (cachedApi != null && lastUrl == formattedUrl) {
                return cachedApi
            }
            try {
                val api = Retrofit.Builder()
                    .baseUrl(formattedUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ShardMailApi::class.java)
                cachedApi = api
                lastUrl = formattedUrl
                api
            } catch (e: Exception) {
                null
            }
        }
    }
}
