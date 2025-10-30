package com.example.tuchanguito.network

import com.example.tuchanguito.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiClient {
    @Volatile private var retrofit: Retrofit? = null

    fun get(baseUrl: String = BuildConfig.BASE_URL, tokenProvider: () -> String? = { null }): Retrofit {
        return retrofit ?: synchronized(this) {
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            val authInterceptor = Interceptor { chain ->
                val req = chain.request().newBuilder().apply {
                    tokenProvider()?.let { header("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(req)
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .build()

            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

            Retrofit.Builder()
                .baseUrl(baseUrl.trimEnd('/') + "/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(client)
                .build()
                .also { retrofit = it }
        }
    }
}
