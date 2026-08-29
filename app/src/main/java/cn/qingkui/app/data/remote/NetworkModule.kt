package cn.qingkui.app.data.remote

import cn.qingkui.app.BuildConfig
import cn.qingkui.app.data.auth.TokenStore
import cn.qingkui.app.data.remote.dto.AuthResponse
import cn.qingkui.app.data.remote.dto.RefreshRequest
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.accessToken() }
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}

class RefreshAuthenticator(
    private val tokenStore: TokenStore,
    private val gson: Gson,
) : Authenticator {
    private val refreshClient = OkHttpClient.Builder().build()
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.endsWith("/auth/refresh") || responseCount(response) > 1) return null
        synchronized(lock) {
            val requestToken = response.request.header("Authorization")?.removePrefix("Bearer ")
            val latestToken = runBlocking { tokenStore.accessToken() }
            if (!latestToken.isNullOrBlank() && latestToken != requestToken) {
                return response.request.newBuilder().header("Authorization", "Bearer $latestToken").build()
            }
            val refreshToken = runBlocking { tokenStore.refreshToken() } ?: return null
            val body = gson.toJson(RefreshRequest(refreshToken)).toRequestBody(JSON)
            val refreshRequest = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "auth/refresh")
                .post(body)
                .build()
            val refreshed = refreshClient.newCall(refreshRequest).execute()
            refreshed.use {
                if (!it.isSuccessful) {
                    runBlocking { tokenStore.clear() }
                    return null
                }
                val auth = gson.fromJson(it.body?.charStream(), AuthResponse::class.java) ?: return null
                runBlocking { tokenStore.save(auth) }
                return response.request.newBuilder().header("Authorization", "Bearer ${auth.accessToken}").build()
            }
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

object NetworkModule {
    fun create(tokenStore: TokenStore): QingkuiApi {
        val gson = Gson()
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logger)
            .authenticator(RefreshAuthenticator(tokenStore, gson))
            .readTimeout(2, TimeUnit.MINUTES)
            .callTimeout(2, TimeUnit.MINUTES)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(QingkuiApi::class.java)
    }
}
