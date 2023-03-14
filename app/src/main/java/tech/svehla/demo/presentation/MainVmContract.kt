package tech.svehla.demo.presentation

import androidx.annotation.StringRes
import tech.svehla.demo.presentation.model.ErrorVO
import tech.svehla.demo.presentation.model.PaymentProgressVO
import tech.svehla.demo.presentation.model.ReaderVO

object MainVmContract {
    data class UiState(
        val isLoading: Boolean = false,
        val paymentProgress: PaymentProgressVO? = null,
        val showPaymentSuccess: Boolean = false,

        val paymentReady: Boolean = false,

        val readers: List<ReaderVO>? = null,
        val amount: String = "",
        @StringRes val amountErrorId: Int? = null,
        val referenceNumber: String = "",
        @StringRes val referenceNumberErrorId: Int? = null,

        val errorVO: ErrorVO? = null,
        val events: List<UiEvent> = emptyList(),
    )

    sealed class UiEvent(val id: String = java.util.UUID.randomUUID().toString()) {
        object NavigateBack : UiEvent()
    }

    sealed class UiAction {
        object OnErrorConsumed : UiAction()
        object OnDiscoverReadersRequested : UiAction()
        object OnStartPaymentRequested : UiAction()
        data class OnAmountChanged(val amount: String) : UiAction()
        data class OnReferenceNumberChanged(val referenceNumber: String) : UiAction()
        data class OnReaderSelected(val reader: ReaderVO) : UiAction()
    }
}