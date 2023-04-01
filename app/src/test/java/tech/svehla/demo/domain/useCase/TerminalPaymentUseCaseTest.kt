package tech.svehla.demo.domain.useCase

import app.cash.turbine.test
import com.google.common.truth.Truth
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.svehla.demo.data.ApiClient
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.domain.model.PaymentProgress
import tech.svehla.demo.testUtil.MainCoroutineExtension

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineExtension::class)
class TerminalPaymentUseCaseTest {

    @MockK
    private lateinit var terminalClient: TerminalClient

    @MockK
    private lateinit var apiClient: ApiClient

    private lateinit var useCase: TerminalPaymentUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        useCase = TerminalPaymentUseCase(terminalClient, apiClient)
    }

    @Test
    fun `On invoke should create payment intent`() = runTest {
        coEvery { apiClient.createPaymentIntent(any(), any(), any()) } returns mockk()
        coEvery { terminalClient.retrievePaymentIntent(any()) } returns mockk()
        coEvery { terminalClient.collectPaymentMethod(any()) } returns mockk()
        coEvery { terminalClient.processPayment(any()) } returns mockk() { every { id } returns "1" }
        coEvery { apiClient.capturePaymentIntent(any()) } returns mockk()

        val result = useCase.startPayment(2000, "czk", "123")

        result.test {
            var progress = awaitItem()
            Truth.assertThat(progress).isInstanceOf(PaymentProgress.PreparingPayment::class.java)
            coVerify { apiClient.createPaymentIntent(any(), any(), any()) }
            coVerify { terminalClient.retrievePaymentIntent(any()) }
            coVerify { terminalClient.collectPaymentMethod(any()) }

            progress = awaitItem()
            Truth.assertThat(progress).isInstanceOf(PaymentProgress.PaymentInProgress::class.java)
            coVerify { terminalClient.processPayment(any()) }
            coVerify { apiClient.capturePaymentIntent(any()) }

            progress = awaitItem()
            Truth.assertThat(progress).isInstanceOf(PaymentProgress.PaymentCompleted::class.java)

            awaitComplete()
        }
    }

    @Test
    fun `On invoke should wrap exception and rethrow if exception is thrown from terminal client`() = runTest {
        coEvery { apiClient.createPaymentIntent(any(), any(), any()) } returns mockk()
        coEvery { terminalClient.retrievePaymentIntent(any()) } throws Exception()

        val result = useCase.startPayment(2000, "czk", "123")

        result.test {
            val progress = awaitItem()
            Truth.assertThat(progress).isInstanceOf(PaymentProgress.PreparingPayment::class.java)
            coVerify { apiClient.createPaymentIntent(any(), any(), any()) }
            coVerify { terminalClient.retrievePaymentIntent(any()) }

            val error = awaitError()
            Truth.assertThat(error).isNotNull()
        }
    }

    @Test
    fun `On invoke should wrap exception and rethrow if exception is thrown from api client`() = runTest {
        coEvery { apiClient.createPaymentIntent(any(), any(), any()) } throws Exception()

        val result = useCase.startPayment(2000, "czk", "123")

        result.test {
            val progress = awaitItem()
            Truth.assertThat(progress).isInstanceOf(PaymentProgress.PreparingPayment::class.java)
            coVerify { apiClient.createPaymentIntent(any(), any(), any()) }

            val error = awaitError()
            Truth.assertThat(error).isNotNull()
        }
    }
}