package com.example.tuchanguito.data.network.api

import com.example.tuchanguito.BuildConfig
import com.example.tuchanguito.data.network.api.AuthApiService
import com.example.tuchanguito.data.network.api.PantryApiService
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object RetrofitClient {
    @Volatile
    private var instance: Retrofit? = null
    @Volatile
    private var tokenProvider: (() -> String?)? = null

    fun configureTokenProvider(provider: () -> String?) {
        tokenProvider = provider
        instance = null
    }

    private fun getInstance(): Retrofit =
        instance ?: synchronized(this) {
            instance ?: buildRetrofit().also { instance = it }
        }

    private fun buildRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder().apply {
                tokenProvider?.invoke()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .build()

        val json = Json { ignoreUnknownKeys = true }

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(okHttpClient)
            .build()
    }

    fun getCategoryApiService(): CategoryApiService =
        getInstance().create(CategoryApiService::class.java)

    fun getProductApiService(): ProductApiService =
        getInstance().create(ProductApiService::class.java)

    fun getShoppingListsApiService(): ShoppingListsApiService =
        getInstance().create(ShoppingListsApiService::class.java)

    fun getPantryApiService(): PantryApiService =
        getInstance().create(PantryApiService::class.java)

    fun getAuthApiService(): AuthApiService =
        getInstance().create(AuthApiService::class.java)
}
