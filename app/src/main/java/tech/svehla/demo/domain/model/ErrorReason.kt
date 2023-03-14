package tech.svehla.demo.domain.model

sealed class ErrorReason {
    data class NetworkError(val message: String) : ErrorReason()
    object NotFound : ErrorReason()
    object AccessDenied : ErrorReason()
    object ServiceUnavailable : ErrorReason()

    data class PaymentError(val message: String? = null) : ErrorReason()
    data class TerminalError(val message: String? = null) : ErrorReason()
    data class ApiError(val message: String? = null) : ErrorReason()
    data class Unknown(val message: String? = null) : ErrorReason()
    object ReaderConnectionError : ErrorReason()
    object ReaderDiscoveryError : ErrorReason()
}