package tech.svehla.demo.presentation

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth
import com.stripe.stripeterminal.external.models.Reader
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.data.ApiClient
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.domain.model.ErrorReason
import tech.svehla.demo.domain.model.PaymentProgress
import tech.svehla.demo.domain.useCase.ConnectToReaderUseCase
import tech.svehla.demo.domain.useCase.DiscoverReadersUseCase
import tech.svehla.demo.domain.useCase.TerminalPaymentUseCase
import tech.svehla.demo.presentation.model.toErrorVO
import tech.svehla.demo.presentation.model.toVO
import tech.svehla.demo.testUtil.MainCoroutineExtension
import tech.svehla.demo.R

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineExtension::class)
class MainVMTest {

    @MockK
    private lateinit var validator: Validator

    @MockK
    private lateinit var terminalClient: TerminalClient

    @MockK
    private lateinit var apiClient: ApiClient

    @MockK
    private lateinit var errorMapper: ErrorMapper

    @MockK
    private lateinit var terminalPaymentUseCase: TerminalPaymentUseCase

    @MockK
    private lateinit var connectToReaderUseCase: ConnectToReaderUseCase

    @MockK
    private lateinit var discoverReadersUseCase: DiscoverReadersUseCase


    private lateinit var viewModel: MainVM

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        viewModel = MainVM(
            validator,
            terminalClient,
            apiClient,
            errorMapper,
            terminalPaymentUseCase,
            connectToReaderUseCase,
            discoverReadersUseCase,
        )
    }

    @Test
    fun `initializeStripeSDK() should init SDK and update reader connection state`() {
        every { terminalClient.initTerminal(any(), any()) } returns Unit
        every { terminalClient.getConnectedReader() } returns null
        val context = mockk<Context>()

        viewModel.initializeStripeSDK(context)

        verify { terminalClient.initTerminal(context, any()) }
        verify { terminalClient.getConnectedReader() }
        with(viewModel.uiState.value) {
            Truth.assertThat(paymentReady).isFalse()
            Truth.assertThat(isLoading).isFalse()
        }
    }

    @Test
    fun `initializeStripeSDK() should set error reason if SDK initialization fails`() {
        val errorReason = ErrorReason.Unknown()
        every { terminalClient.initTerminal(any(), any()) } throws Exception()
        every { terminalClient.getConnectedReader() } returns null
        every { errorMapper.mapException(any()) } returns errorReason
        val context = mockk<Context>()

        viewModel.initializeStripeSDK(context)

        verify { terminalClient.initTerminal(context, any()) }
        verify { errorMapper.mapException(any()) }
        Truth.assertThat(viewModel.uiState.value.errorVO).isEqualTo(errorReason.toErrorVO())
    }

    @Test
    fun `isStripeSDKInitialized() should return true if SDK is initialized`() {
        every { terminalClient.isInitialized() } returns true

        val result = viewModel.isStripeSDKInitialized()

        Truth.assertThat(result).isTrue()
    }

    @Test
    fun `On OnDiscoverReadersRequested action received should start discovery and update the UI state`() = runTest {
        val reader = mockk<Reader>(relaxed = true)
        val discoverFlow = MutableSharedFlow<List<Reader>>()
        every { discoverReadersUseCase.discoverReaders() } returns discoverFlow

        viewModel.onUiAction(MainVmContract.UiAction.OnDiscoverReadersRequested)
        advanceUntilIdle()

        verify { discoverReadersUseCase.discoverReaders() }
        discoverFlow.test {
            Truth.assertThat(viewModel.uiState.value.isLoading).isTrue()

            discoverFlow.emit(emptyList())
            awaitItem()
            with(viewModel.uiState.value) {
                Truth.assertThat(isLoading).isFalse()
                Truth.assertThat(readerVOs).isEmpty()
            }

            discoverFlow.emit(listOf(reader))
            awaitItem()
            with(viewModel.uiState.value) {
                Truth.assertThat(isLoading).isFalse()
                Truth.assertThat(readerVOs).isEqualTo(listOf(reader.toVO()))
            }
        }
    }

    @Test
    fun `On OnDiscoverReadersRequested action handling should set error reason if discovery fails`() = runTest {
        val errorReason = ErrorReason.Unknown()
        val discoverFlow = flow<List<Reader>> { throw Exception() }
        every { discoverReadersUseCase.discoverReaders() } returns discoverFlow
        every { errorMapper.mapException(any()) } returns errorReason

        viewModel.onUiAction(MainVmContract.UiAction.OnDiscoverReadersRequested)
        advanceUntilIdle()

        verify { discoverReadersUseCase.discoverReaders() }
        discoverFlow.test {
            awaitError()
            with(viewModel.uiState.value) {
                Truth.assertThat(isLoading).isFalse()
                Truth.assertThat(errorVO).isEqualTo(errorReason.toErrorVO())
            }
        }
    }

    @Test
    fun `On OnConnectToReaderRequested action received should connect to reader and update the UI state`() = runTest {
        val reader = mockk<Reader>(relaxed = true) { every { serialNumber } returns "123" }
        coEvery { connectToReaderUseCase(any()) } returns Unit
        every { discoverReadersUseCase.discoverReaders() } returns flow { emit(listOf(reader)) }
        viewModel.onUiAction(MainVmContract.UiAction.OnDiscoverReadersRequested)
        advanceUntilIdle()

        viewModel.onUiAction(MainVmContract.UiAction.OnReaderSelected("123"))
        advanceUntilIdle()

        coVerify { connectToReaderUseCase(reader) }
        with(viewModel.uiState.value) {
            Truth.assertThat(isLoading).isFalse()
            Truth.assertThat(paymentReady).isTrue()
        }
    }

    @Test
    fun `On OnConnectToReaderRequested action handling should set error reason if connection fails`() = runTest {
        val errorReason = ErrorReason.Unknown()
        val reader = mockk<Reader>(relaxed = true) { every { serialNumber } returns "123" }
        coEvery { connectToReaderUseCase(any()) } throws Exception()
        every { discoverReadersUseCase.discoverReaders() } returns flow { emit(listOf(reader)) }
        every { errorMapper.mapException(any()) } returns errorReason
        viewModel.onUiAction(MainVmContract.UiAction.OnDiscoverReadersRequested)
        advanceUntilIdle()

        viewModel.onUiAction(MainVmContract.UiAction.OnReaderSelected("123"))
        advanceUntilIdle()

        coVerify { connectToReaderUseCase(reader) }
        with(viewModel.uiState.value) {
            Truth.assertThat(isLoading).isFalse()
            Truth.assertThat(errorVO).isEqualTo(errorReason.toErrorVO())
        }
    }

    @Test
    fun `On OnPaymentRequested action received should start payment and update the UI state`() = runTest {
        val amountStr = "20.00"
        val amountInt = 2000
        val currency = "czk"
        val referenceNumber = "123"
        val paymentFlow = flow {
            delay(100)
            emit(PaymentProgress.PaymentInProgress)
            delay(100)
            emit(PaymentProgress.PaymentCompleted)
        }
        coEvery { terminalPaymentUseCase.startPayment(amountInt, currency, referenceNumber) } returns paymentFlow
        every { validator.validateAmount(any()) } returns null
        every { validator.validateReferenceNumber(any()) } returns null
        every { validator.convertAmount(any()) } returns amountInt
        viewModel.onUiAction(MainVmContract.UiAction.OnAmountChanged(amountStr))
        viewModel.onUiAction(MainVmContract.UiAction.OnReferenceNumberChanged(referenceNumber))

        viewModel.onUiAction(MainVmContract.UiAction.OnPaymentRequested)

        advanceTimeBy(50)
        Truth.assertThat(viewModel.uiState.value.isLoading).isTrue()

        advanceTimeBy(100)
        with(viewModel.uiState.value) {
            Truth.assertThat(isLoading).isTrue()
            Truth.assertThat(paymentProgress).isEqualTo(PaymentProgress.PaymentInProgress.toVO())
            Truth.assertThat(showPaymentSuccess).isFalse()
        }

        advanceTimeBy(100)
        with(viewModel.uiState.value) {
            Truth.assertThat(isLoading).isFalse()
            Truth.assertThat(paymentProgress).isEqualTo(PaymentProgress.PaymentCompleted.toVO())
            Truth.assertThat(showPaymentSuccess).isTrue()
        }

        coVerify { terminalPaymentUseCase.startPayment(amountInt, currency, referenceNumber) }
    }

    @Test
    fun `On OnPaymentRequested action handling should set error reason if payment fails`() = runTest {
        val errorReason = ErrorReason.Unknown()
        val paymentFlow = flow<PaymentProgress> {
            throw Exception()
        }
        coEvery { terminalPaymentUseCase.startPayment(2000, "czk", "123") } returns paymentFlow
        every { validator.validateAmount(any()) } returns null
        every { validator.validateReferenceNumber(any()) } returns null
        every { validator.convertAmount(any()) } returns 2000
        every { errorMapper.mapException(any()) } returns errorReason
        viewModel.onUiAction(MainVmContract.UiAction.OnAmountChanged("20.00"))
        viewModel.onUiAction(MainVmContract.UiAction.OnReferenceNumberChanged("123"))

        viewModel.onUiAction(MainVmContract.UiAction.OnPaymentRequested)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            Truth.assertThat(isLoading).isFalse()
            Truth.assertThat(errorVO).isEqualTo(errorReason.toErrorVO())
            Truth.assertThat(showPaymentSuccess).isFalse()
        }

        coVerify { terminalPaymentUseCase.startPayment(any(), any(), any()) }
    }

    @Test
    fun `On OnPaymentRequested action handling should not start payment if amount is invalid`() = runTest {
        every { validator.validateAmount(any()) } returns R.string.error_amount_invalid
        every { validator.validateReferenceNumber(any()) } returns null

        viewModel.onUiAction(MainVmContract.UiAction.OnAmountChanged("abc"))
        viewModel.onUiAction(MainVmContract.UiAction.OnPaymentRequested)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            Truth.assertThat(isLoading).isFalse()
            Truth.assertThat(paymentProgress).isNull()
            Truth.assertThat(showPaymentSuccess).isFalse()
        }

        coVerify(exactly = 0) { terminalPaymentUseCase.startPayment(any(), any(), any()) }
    }

    @Test
    fun `On OnPaymentRequested action handling should not start payment if reference number is invalid`() = runTest {
        every { validator.validateAmount(any()) } returns null
        every { validator.validateReferenceNumber(any()) } returns R.string.error_reference_invalid

        viewModel.onUiAction(MainVmContract.UiAction.OnReferenceNumberChanged("abc"))
        viewModel.onUiAction(MainVmContract.UiAction.OnPaymentRequested)
        advanceUntilIdle()

        with(viewModel.uiState.value) {
            Truth.assertThat(isLoading).isFalse()
            Truth.assertThat(paymentProgress).isNull()
            Truth.assertThat(showPaymentSuccess).isFalse()
        }

        coVerify(exactly = 0) { terminalPaymentUseCase.startPayment(any(), any(), any()) }
    }
}