package com.nodimobilebanking.data.api

import android.content.Context
import com.nodimobilebanking.BuildConfig
import com.nodimobilebanking.utils.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    @Volatile private var instance: SaccoApiService? = null

    fun getInstance(tokenManager: TokenManager, context: Context? = null): SaccoApiService {
        if (instance == null) {
            synchronized(this) {
                if (instance == null) {
                    val logging = HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG)
                            HttpLoggingInterceptor.Level.HEADERS
                        else
                            HttpLoggingInterceptor.Level.NONE
                    }

                    val builder = OkHttpClient.Builder()
                        .addInterceptor(AuthInterceptor(tokenManager))
                        .addInterceptor(logging)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)

                    // Add token authenticator for auto-refresh/logout
                    if (context != null) {
                        builder.authenticator(TokenAuthenticator(context.applicationContext, tokenManager))
                    }

                    instance = Retrofit.Builder()
                        .baseUrl(BuildConfig.BASE_URL)
                        .client(builder.build())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(SaccoApiService::class.java)
                }
            }
        }
        return instance!!
    }

    @Volatile private var publicInstance: SaccoApiService? = null

    fun getPublicInstance(context: Context): SaccoApiService {
        if (publicInstance == null) {
            synchronized(this) {
                if (publicInstance == null) {
                    val logging = HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG)
                            HttpLoggingInterceptor.Level.HEADERS
                        else
                            HttpLoggingInterceptor.Level.NONE
                    }
                    val client = OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()

                    publicInstance = Retrofit.Builder()
                        .baseUrl(BuildConfig.BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(SaccoApiService::class.java)
                }
            }
        }
        return publicInstance!!
    }

    /** Call when user logs out to force new client on next login */
    fun reset() {
        instance = null
    }
}
