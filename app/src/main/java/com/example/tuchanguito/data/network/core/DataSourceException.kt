package com.example.tuchanguito.data.network.core

/**
 * Common exception thrown by remote data sources when an API request fails.
 */
class DataSourceException(
    val code: Code,
    val statusCode: Int? = null,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    enum class Code {
        UNAUTHORIZED,
        DATA,
        CONNECTION,
        UNEXPECTED
    }
}
