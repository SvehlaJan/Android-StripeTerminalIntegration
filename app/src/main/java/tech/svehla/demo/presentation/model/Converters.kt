package tech.svehla.demo.presentation.model

import com.stripe.stripeterminal.external.models.Reader
import tech.svehla.demo.R
import tech.svehla.demo.domain.model.ErrorReason
import tech.svehla.demo.domain.model.PaymentProgress

fun Reader.toVO() = ReaderVO(
    serialNumber = serialNumber,
    label = label,
    ipAddress = ipAddress,
)

fun PaymentProgress.toVO() = when (this) {
    is PaymentProgress.PreparingPayment -> PaymentProgressVO(messageId = R.string.payment_preparing)
    is PaymentProgress.PaymentInProgress -> PaymentProgressVO(messageId = R.string.payment_in_progress)
    is PaymentProgress.PaymentCompleted -> PaymentProgressVO(messageId = R.string.payment_success)
}

fun ErrorReason.toErrorVO(): ErrorVO {
    return when (this) {
        is ErrorReason.NetworkError -> ErrorVO(
            titleRes = R.string.error_general_title,
            messageRes = R.string.error_network
        )

        is ErrorReason.NotFound -> ErrorVO(
            titleRes = R.string.error_general_title,
            messageRes = R.string.error_not_found
        )

        is ErrorReason.AccessDenied -> ErrorVO(
            titleRes = R.string.error_general_title,
            messageRes = R.string.error_access_denied
        )

        is ErrorReason.ServiceUnavailable -> ErrorVO(
            titleRes = R.string.error_general_title,
            messageRes = R.string.error_service_unavailable
        )

        is ErrorReason.PaymentError -> ErrorVO(
            titleRes = R.string.error_general_title,
            message = message
        )

        is ErrorReason.Unknown -> ErrorVO(
            titleRes = R.string.error_general_title,
            message = message
        )

        is ErrorReason.ReaderConnectionError -> ErrorVO(
            titleRes = R.string.error_general_title,
            message = "Failed to connect to reader"
        )

        is ErrorReason.ReaderDiscoveryError -> ErrorVO(
            titleRes = R.string.error_general_title,
            message = "Error while discovering readers"
        )

        is ErrorReason.TerminalError -> ErrorVO(
            titleRes = R.string.error_terminal_title,
            message = message
        )

        is ErrorReason.ApiError -> ErrorVO(
            titleRes = R.string.error_api_title,
            message = message
        )
    }
}