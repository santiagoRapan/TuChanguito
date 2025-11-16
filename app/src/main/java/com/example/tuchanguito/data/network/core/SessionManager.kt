package com.example.tuchanguito.data.network.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object SessionManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clearing = AtomicBoolean(false)
    @Volatile
    private var handler: (suspend () -> Unit)? = null

    fun configure(onUnauthorized: suspend () -> Unit) {
        handler = onUnauthorized
    }

    fun handleUnauthorized() {
        val action = handler ?: return
        if (!clearing.compareAndSet(false, true)) return
        scope.launch {
            try {
                action()
            } finally {
                clearing.set(false)
            }
        }
    }
}
