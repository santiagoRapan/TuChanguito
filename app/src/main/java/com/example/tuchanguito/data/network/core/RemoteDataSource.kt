package com.example.tuchanguito.data.network.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

/**
 * Base class that encapsulates consistent error handling for all network calls.
 */
abstract class RemoteDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    protected suspend fun <T> safeApiCall(block: suspend () -> T): T {
        return try {
            block()
        } catch (error: DataSourceException) {
            throw error
        } catch (error: HttpException) {
            throw mapHttpException(error)
        } catch (error: IOException) {
            throw DataSourceException(
                code = DataSourceException.Code.CONNECTION,
                statusCode = null,
                message = error.message ?: "Error de conexión",
                cause = error
            )
        } catch (error: Exception) {
            throw DataSourceException(
                code = DataSourceException.Code.UNEXPECTED,
                statusCode = null,
                message = error.message ?: "Error inesperado",
                cause = error
            )
        }
    }

    private fun mapHttpException(exception: HttpException): DataSourceException {
        val body = exception.response()?.errorBody()?.peekString()
        val message = body?.let { parseMessage(it) } ?: exception.message()
        val statusCode = exception.code()
        val code = when (statusCode) {
            400, 404, 409, 422 -> DataSourceException.Code.DATA
            401, 403 -> DataSourceException.Code.UNAUTHORIZED
            else -> DataSourceException.Code.UNEXPECTED
        }
        return DataSourceException(
            code = code,
            statusCode = statusCode,
            message = message.ifBlank { "Error de servidor" },
            cause = exception
        )
    }

    private fun parseMessage(raw: String): String {
        return runCatching { json.decodeFromString<ErrorPayload>(raw) }
            .map { payload ->
                payload.message
                    ?: payload.error
                    ?: payload.errors?.values?.firstOrNull()?.firstOrNull()
            }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: raw.take(200)
    }

    private fun ResponseBody.peekString(): String = use { it.string() }

    @Serializable
    private data class ErrorPayload(
        val message: String? = null,
        val error: String? = null,
        val errors: Map<String, List<String>>? = null,
        @SerialName("detail") val detail: String? = null
    )
}
