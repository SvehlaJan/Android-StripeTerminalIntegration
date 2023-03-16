package tech.svehla.demo.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.stripeterminal.external.models.Reader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.data.ApiClient
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.domain.model.ErrorReason
import tech.svehla.demo.domain.model.PaymentProgress
import tech.svehla.demo.domain.useCase.ConnectToReaderException
import tech.svehla.demo.domain.useCase.ConnectToReaderUseCase
import tech.svehla.demo.domain.useCase.DiscoverReadersException
import tech.svehla.demo.domain.useCase.DiscoverReadersUseCase
import tech.svehla.demo.domain.useCase.TerminalPaymentException
import tech.svehla.demo.domain.useCase.TerminalPaymentUseCase
import tech.svehla.demo.presentation.model.toErrorVO
import tech.svehla.demo.presentation.model.toVO
import timber.log.Timber

// TODO - replace with DI
class MainVM(
    private val inputValidator: Validator = Validator(),
    private val terminalClient: TerminalClient = TerminalClient(),
    private val apiClient: ApiClient = ApiClient.getInstance(),
    private val errorMapper: ErrorMapper = ErrorMapper(),
    private val terminalPaymentUseCase: TerminalPaymentUseCase = TerminalPaymentUseCase(terminalClient, apiClient, errorMapper),
    private val connectToReaderUseCase: ConnectToReaderUseCase = ConnectToReaderUseCase(terminalClient, errorMapper),
    private val discoverReadersUseCase: DiscoverReadersUseCase = DiscoverReadersUseCase(terminalClient, errorMapper),
) : ViewModel() {

    private val _uiState: MutableStateFlow<MainVmContract.UiState> = MutableStateFlow(MainVmContract.UiState())
    val uiState: MutableStateFlow<MainVmContract.UiState> = _uiState

    private var availableReaders: List<Reader> = emptyList()

    fun initializeStripeSDK(applicationContext: Context) {
        try {
            terminalClient.initTerminal(applicationContext, onTerminalDisconnected = {
                updateReaderConnection(false)
            })
        } catch (e: Exception) {
            Timber.e("Error while initializing Stripe SDK: $e")
            _uiState.update { it.copy(errorVO = errorMapper.mapException(e).toErrorVO()) }
        }

        val isConnectedToReader = terminalClient.getConnectedReader() != null
        updateReaderConnection(isConnectedToReader)
    }

    fun isStripeSDKInitialized() = terminalClient.isInitialized()

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

            is MainVmContract.UiAction.OnReaderSelected -> {
                connectToReader(action.serialNumber)
            }

            is MainVmContract.UiAction.OnPaymentRequested -> {
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

    private fun connectToReader(serialNumber: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        val reader = availableReaders.firstOrNull() { it.serialNumber == serialNumber }
        if (reader == null) {
            Timber.e("Reader not found")
            return@launch
        }

        try {
            connectToReaderUseCase(reader)
            updateReaderConnection(true)
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
                        readerVOs = discoveredReaders.map { reader -> reader.toVO() }
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