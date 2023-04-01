package tech.svehla.demo.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.stripeterminal.external.models.Reader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.data.ApiClient
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.domain.model.PaymentProgress
import tech.svehla.demo.domain.useCase.ConnectToReaderUseCase
import tech.svehla.demo.domain.useCase.DiscoverReadersUseCase
import tech.svehla.demo.domain.useCase.TerminalPaymentUseCase
import tech.svehla.demo.domain.useCase.TerminalUseCase
import tech.svehla.demo.presentation.model.toErrorVO
import tech.svehla.demo.presentation.model.toVO
import timber.log.Timber

// TODO - replace with DI
class MainVM(
    private val errorMapper: ErrorMapper = ErrorMapper(),
    private val inputValidator: Validator = Validator(),
    apiClient: ApiClient = ApiClient.getInstance(),
    terminalClient: TerminalClient = TerminalClient(),
    private val terminalUseCase: TerminalUseCase = TerminalUseCase(terminalClient),
    private val terminalPaymentUseCase: TerminalPaymentUseCase = TerminalPaymentUseCase(terminalClient, apiClient),
    private val connectToReaderUseCase: ConnectToReaderUseCase = ConnectToReaderUseCase(terminalClient),
    private val discoverReadersUseCase: DiscoverReadersUseCase = DiscoverReadersUseCase(terminalClient),
) : ViewModel() {

    private val _uiState: MutableStateFlow<MainVmContract.UiState> = MutableStateFlow(MainVmContract.UiState())
    val uiState: MutableStateFlow<MainVmContract.UiState> = _uiState

    private var availableReaders: List<Reader> = emptyList()

    init {
        terminalUseCase.observeConnectedReader()
            .onEach { reader ->
                Timber.d("Connected reader changed: $reader")
                updateReaderConnection(reader != null)
            }
            .catch { e ->
                Timber.e(e, "Error while observing terminal state")
            }
            .launchIn(viewModelScope)
        terminalUseCase.observePaymentStatus()
            .onEach { status ->
                Timber.d("Payment status changed: $status")
            }
            .catch { e ->
                Timber.e(e, "Error while observing payment progress")
            }
            .launchIn(viewModelScope)
        terminalUseCase.observeConnectionStatus()
            .onEach { status ->
                Timber.d("Connection status changed: $status")
            }
            .catch { e ->
                Timber.e(e, "Error while observing connection status")
            }
            .launchIn(viewModelScope)
    }

    fun initializeStripeSDK(applicationContext: Context) = viewModelScope.launch {
        try {
            terminalUseCase.initTerminal(applicationContext)
        } catch (e: Exception) {
            Timber.e("Error while initializing Stripe SDK: $e")
            _uiState.update { it.copy(errorVO = errorMapper.mapException(e).toErrorVO()) }
        }
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
            connectToReaderUseCase.connect(reader)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to reader")
            _uiState.update { it.copy(
                isLoading = false,
                errorVO = errorMapper.mapException(e).toErrorVO()
            ) }
        }
    }

    private fun discoverReaders() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }

        discoverReadersUseCase.discoverReaders()
            .catch { e ->
                Timber.e(e, "Failed to discover readers")
                _uiState.update { it.copy(
                    isLoading = false,
                    errorVO = errorMapper.mapException(e).toErrorVO()
                ) }
            }
            .collect() { discoveredReaders ->
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
                _uiState.update { it.copy(
                    isLoading = false,
                    errorVO = errorMapper.mapException(e).toErrorVO()
                ) }
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