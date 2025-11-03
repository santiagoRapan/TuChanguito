package com.example.tuchanguito.network

import android.content.Context
import com.example.tuchanguito.data.PreferencesManager
import com.example.tuchanguito.network.service.AuthService
import com.example.tuchanguito.network.service.CatalogService
import com.example.tuchanguito.network.service.ShoppingService
import com.example.tuchanguito.network.service.PantryService
import com.example.tuchanguito.network.service.ShoppingPatchService
import kotlinx.coroutines.flow.firstOrNull

/** Simple provider for API services.  */
class ApiModule(context: Context) {
    private val prefs = PreferencesManager(context)
    private val retrofit by lazy {
        ApiClient.get(tokenProvider = { runBlockingToken() })
    }

    private fun runBlockingToken(): String? {
        // Read the latest token synchronously for interceptor; acceptable since it's cached in memory by DataStore
        return runCatching {
            kotlinx.coroutines.runBlocking { prefs.authToken.firstOrNull() }
        }.getOrNull()
    }

    val auth: AuthService by lazy { retrofit.create(AuthService::class.java) }
    val catalog: CatalogService by lazy { retrofit.create(CatalogService::class.java) }
    val shopping: ShoppingService by lazy { retrofit.create(ShoppingService::class.java) }
    val pantry: PantryService by lazy { retrofit.create(PantryService::class.java) }
    val shoppingPatch: ShoppingPatchService by lazy { retrofit.create(ShoppingPatchService::class.java) }
}
