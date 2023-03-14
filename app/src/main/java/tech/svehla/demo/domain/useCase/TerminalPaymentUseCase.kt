package tech.svehla.demo.domain.useCase

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import tech.svehla.demo.data.model.ServerPaymentIntent
import tech.svehla.demo.data.ApiClient
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.domain.model.ErrorReason
import tech.svehla.demo.domain.model.PaymentProgress
import timber.log.Timber
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TerminalPaymentUseCase {

    private suspend fun processPayment(paymentIntent: PaymentIntent) = suspendCoroutine<PaymentIntent> { continuation ->
        Terminal.getInstance().processPayment(paymentIntent,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Timber.d("Payment processed successfully")
                    continuation.resumeWith(Result.success(paymentIntent))
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Error processing payment")
                    continuation.resumeWithException(TerminalPaymentException(ErrorMapper.mapException(e)))
                }
            }
        )
    }

    private suspend fun collectPaymentMethod(paymentIntent: PaymentIntent) = suspendCoroutine<PaymentIntent> { continuation ->
        Terminal.getInstance().collectPaymentMethod(paymentIntent,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Timber.d("Payment method collected successfully")
                    continuation.resumeWith(Result.success(paymentIntent))
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Error collecting payment method")
                    continuation.resumeWithException(TerminalPaymentException(ErrorMapper.mapException(e)))
                }
            }
        )
    }

    private suspend fun retrievePaymentIntent(intent: ServerPaymentIntent) = suspendCoroutine<PaymentIntent> { continuation ->
        Terminal.getInstance().retrievePaymentIntent(intent.secret,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Timber.d("Payment intent retrieved successfully")
                    continuation.resumeWith(Result.success(paymentIntent))
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Error retrieving payment intent")
                    continuation.resumeWithException(TerminalPaymentException(ErrorMapper.mapException(e)))
                }
            }
        )
    }

    suspend fun startPayment(amount: Int, currency: String, referenceNumber: String) = flow {
        try {
            emit(PaymentProgress.PreparingPayment)
            Timber.d("Starting payment")
            val intent = ApiClient.createPaymentIntent(amount, currency, referenceNumber)

            Timber.d("Retrieving payment intent")
            var paymentIntent = retrievePaymentIntent(intent)

            Timber.d("Collecting payment method")
            paymentIntent = collectPaymentMethod(paymentIntent)

            emit(PaymentProgress.PaymentInProgress)
            Timber.d("Processing payment")
            paymentIntent = processPayment(paymentIntent)

            Timber.d("Capturing payment intent")
            ApiClient.capturePaymentIntent(paymentIntent.id)
            emit(PaymentProgress.PaymentCompleted)
        } catch (e: Exception) {
            Timber.e(e, "Error processing payment")
            throw TerminalPaymentException(ErrorMapper.mapException(e))
        }
    }.flowOn(Dispatchers.IO)
}

class TerminalPaymentException(val errorReason: ErrorReason) : Exception("Error processing payment")