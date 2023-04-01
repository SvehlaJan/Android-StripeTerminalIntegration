package tech.svehla.demo.api

import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.stripe.stripeterminal.external.models.TerminalException
import tech.svehla.demo.domain.model.ErrorReason

class ErrorMapper {
    fun mapException(throwable: Throwable): ErrorReason {
        return when (throwable) {
            is TerminalException -> {
                val errorCode = throwable.errorCode // TODO - more granular error handling
                ErrorReason.TerminalError(throwable.errorMessage)
            }
            is ConnectionTokenException -> {
                ErrorReason.ApiError(throwable.message)
            }

            is retrofit2.HttpException -> {
                val errorBody = throwable.response()?.errorBody()?.string()
                when (throwable.code()) {
                    404 -> ErrorReason.NotFound
                    401 -> ErrorReason.AccessDenied
                    503 -> ErrorReason.ServiceUnavailable
                    else -> ErrorReason.ApiError(errorBody ?: throwable.message)
                }
            }

            is java.net.SocketTimeoutException -> ErrorReason.NetworkError(throwable.message ?: "Timeout")
            is java.net.UnknownHostException -> ErrorReason.NetworkError(throwable.message ?: "Unknown host")
            is java.net.ConnectException -> ErrorReason.NetworkError(throwable.message ?: "Connection error")
            is java.net.SocketException -> ErrorReason.NetworkError(throwable.message ?: "Socket error")

            else -> {
                ErrorReason.Unknown(throwable.message)
            }
        }
    }
}