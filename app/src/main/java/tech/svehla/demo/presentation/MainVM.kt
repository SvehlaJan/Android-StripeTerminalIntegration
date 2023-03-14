package tech.svehla.demo.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.svehla.demo.data.TerminalEventListener
import tech.svehla.demo.data.TokenProvider
import tech.svehla.demo.domain.model.ErrorReason
import tech.svehla.demo.domain.model.PaymentProgress
import tech.svehla.demo.domain.useCase.ConnectToReaderException
import tech.svehla.demo.domain.useCase.ConnectToReaderUseCase
import tech.svehla.demo.domain.useCase.DiscoverReadersException
import tech.svehla.demo.domain.useCase.DiscoverReadersUseCase
import tech.svehla.demo.domain.useCase.TerminalPaymentException
import tech.svehla.demo.domain.useCase.TerminalPaymentUseCase
import tech.svehla.demo.presentation.model.ReaderVO
import tech.svehla.demo.presentation.model.toErrorVO
import tech.svehla.demo.presentation.model.toVO
import timber.log.Timber

class MainVM(
    private val inputValidator: Validator = Validator(),
    private val terminalPaymentUseCase: TerminalPaymentUseCase = TerminalPaymentUseCase(),
    private val connectToReaderUseCase: ConnectToReaderUseCase = ConnectToReaderUseCase(),
    private val discoverReadersUseCase: DiscoverReadersUseCase = DiscoverReadersUseCase(),
) : ViewModel() {

    private val _uiState: MutableStateFlow<MainVmContract.UiState> = MutableStateFlow(MainVmContract.UiState())
    val uiState: MutableStateFlow<MainVmContract.UiState> = _uiState

    var availableReaders: List<Reader> = emptyList()

    fun initializeStripeSDK(applicationContext: Context) {
        try {
            Terminal.initTerminal(applicationContext, LogLevel.VERBOSE, TokenProvider(), TerminalEventListener())
        } catch (e: TerminalException) {
            throw RuntimeException(
                "Location services are required in order to initialize the Terminal.", e
            )
        }

        val isConnectedToReader = Terminal.getInstance().connectedReader != null
        updateReaderConnection(isConnectedToReader)
    }

    fun onEventConsumed(event: MainVmContract.UiEvent) {
        _uiState.update { it.copy(events = _uiState.value.events.filter { e -> e.id != event.id }) }
    }

    fun onUiAction(action: MainVmContract.UiAction) {
        when (action) {
            is MainVmContract.UiAction.OnErrorConsumed -> {
                _uiState.update { it.copy(errorVO = null) }
            }

            is MainVmContract.UiAction.OnDiscoverReadersRequested -> {
                discoverReaders()
            }

            is MainVmContract.UiAction.OnStartPaymentRequested -> {
                val amountErrorId = inputValidator.validateAmount(_uiState.value.amount)
                val referenceNumberErrorId = inputValidator.validateReferenceNumber(_uiState.value.referenceNumber)
                if (amountErrorId != null || referenceNumberErrorId != null) {
                    _uiState.update {
                        it.copy(
                            amountErrorId = amountErrorId,
                            referenceNumberErrorId = referenceNumberErrorId,
                        )
                    }
                    return
                }

                startPayment()
            }

            is MainVmContract.UiAction.OnReaderSelected -> {
                connectToReader(action.reader)
            }

            is MainVmContract.UiAction.OnAmountChanged -> {
                val errorId = inputValidator.validateAmount(action.amount)
                _uiState.update {
                    it.copy(
                        amount = action.amount,
                        amountErrorId = errorId,
                    )
                }
            }

            is MainVmContract.UiAction.OnReferenceNumberChanged -> {
                val errorId = inputValidator.validateReferenceNumber(action.referenceNumber)
                _uiState.update {
                    it.copy(
                        referenceNumber = action.referenceNumber,
                        referenceNumberErrorId = errorId,
                    )
                }
            }
        }
    }

    private fun updateReaderConnection(isConnected: Boolean) {
        _uiState.update {
            it.copy(
                paymentReady = isConnected,
                isLoading = false
            )
        }
    }

    private fun connectToReader(readerVO: ReaderVO) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        val reader = availableReaders.firstOrNull() { it.serialNumber == readerVO.serialNumber }
        if (reader == null) {
            Timber.e("Reader not found")
            return@launch
        }

        try {
            val isConnected = connectToReaderUseCase(reader)
            updateReaderConnection(isConnected)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to reader")
            if (e is ConnectToReaderException) {
                _uiState.update { it.copy(isLoading = false, errorVO = e.errorReason.toErrorVO()) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorVO = ErrorReason.Unknown(e.message).toErrorVO()) }
            }
        }
    }

    private fun discoverReaders() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        discoverReadersUseCase.discoverReaders()
            // TODO: Figure out an elegant solution to pause the flow when the app is in background
//            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            .catch { e ->
                Timber.e(e, "Failed to discover readers")
                if (e is DiscoverReadersException) {
                    _uiState.update { it.copy(isLoading = false, errorVO = e.errorReason.toErrorVO()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorVO = ErrorReason.Unknown(e.message).toErrorVO()) }
                }
            }
            .collect { discoveredReaders ->
                availableReaders = discoveredReaders
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        readers = discoveredReaders.map { reader -> reader.toVO() }
                    )
                }
            }
    }

    private fun startPayment() = viewModelScope.launch {
        _uiState.update {
            it.copy(
                isLoading = true,
                paymentProgress = null,
                showPaymentSuccess = false,
            )
        }

        val amount = inputValidator.convertAmount(_uiState.value.amount)
        val referenceNumber = _uiState.value.referenceNumber

        terminalPaymentUseCase.startPayment(amount, "czk", referenceNumber)
            .catch { e ->
                if (e is TerminalPaymentException) {
                    _uiState.update { it.copy(isLoading = false, errorVO = e.errorReason.toErrorVO()) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorVO = ErrorReason.Unknown(e.message).toErrorVO()) }
                }
            }
            .collect { paymentProgress ->
                _uiState.update {
                    it.copy(
                        paymentProgress = paymentProgress.toVO(),
                        showPaymentSuccess = paymentProgress is PaymentProgress.PaymentCompleted,
                    )
                }
            }

        _uiState.update { it.copy(isLoading = false) }
    }
}