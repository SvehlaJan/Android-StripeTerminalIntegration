package tech.svehla.demo.domain.useCase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.data.ApiClient
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.domain.model.ErrorReason
import tech.svehla.demo.domain.model.PaymentProgress
import timber.log.Timber

class TerminalPaymentUseCase(
    private val terminalClient: TerminalClient,
    private val apiClient: ApiClient,
    private val errorMapper: ErrorMapper,
) {
    suspend fun startPayment(amount: Int, currency: String, referenceNumber: String) = flow {
        try {
            emit(PaymentProgress.PreparingPayment)
            Timber.d("Starting payment")
            val intent = apiClient.createPaymentIntent(amount, currency, referenceNumber)

            Timber.d("Retrieving payment intent")
            var paymentIntent = terminalClient.retrievePaymentIntent(intent)

            Timber.d("Collecting payment method")
            paymentIntent = terminalClient.collectPaymentMethod(paymentIntent)

            emit(PaymentProgress.PaymentInProgress)
            Timber.d("Processing payment")
            paymentIntent = terminalClient.processPayment(paymentIntent)

            Timber.d("Capturing payment intent")
            apiClient.capturePaymentIntent(paymentIntent.id)
            emit(PaymentProgress.PaymentCompleted)
        } catch (e: Exception) {
            Timber.e(e, "Error processing payment")
            throw TerminalPaymentException(errorMapper.mapException(e))
        }
    }.flowOn(Dispatchers.IO)
}

class TerminalPaymentException(val errorReason: ErrorReason) : Exception("Error processing payment")