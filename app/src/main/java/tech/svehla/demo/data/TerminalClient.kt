package tech.svehla.demo.data

import android.content.Context
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import tech.svehla.demo.data.model.ServerPaymentIntent
import timber.log.Timber
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TerminalClient {

    fun initTerminal(applicationContext: Context, onTerminalDisconnected: () -> Unit) {
        Terminal.initTerminal(applicationContext, LogLevel.VERBOSE, TokenProvider(), object : TerminalListener {
            override fun onUnexpectedReaderDisconnect(reader: Reader) {
                Timber.d("Reader disconnected unexpectedly: ${reader.serialNumber}")
                onTerminalDisconnected()
            }
        })
    }

    fun isInitialized(): Boolean = Terminal.isInitialized()

    fun getConnectedReader(): Reader? = Terminal.getInstance().connectedReader

    fun discoverReaders(discoveryConfig: DiscoveryConfiguration) = callbackFlow<List<Reader>> {
        val discoveryCallback = object : Callback {
            override fun onSuccess() {
                Timber.d("Discovering readers")
            }

            override fun onFailure(e: TerminalException) {
                Timber.e(e, "Failed to discover readers")
                cancel(CancellationException("Terminal discovery failed", e))
            }
        }

        val discoveryListener = object : DiscoveryListener {
            override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                trySendBlocking(readers).onFailure {
                    Timber.e(it, "Failed to send discovered readers from callback flow")
                }
            }
        }

        val discovery = Terminal.getInstance().discoverReaders(discoveryConfig, discoveryListener, discoveryCallback)

        awaitClose {
            Timber.d("Closing discover readers flow")
            discovery.cancel(object : Callback {
                override fun onSuccess() {
                    Timber.d("Discover readers canceled")
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Failed to cancel discover readers")
                }
            })
        }
    }

    suspend fun connectToReader(
        reader: Reader,
        connectionConfig: ConnectionConfiguration.InternetConnectionConfiguration,
    ) = suspendCoroutine<Unit> { continuation ->
        val readerCallback = object : ReaderCallback {
            override fun onSuccess(reader: Reader) {
                Timber.d("Connected to reader")
                continuation.resumeWith(Result.success(Unit))
            }

            override fun onFailure(e: TerminalException) {
                Timber.e(e, "Failed to connect to reader")
                continuation.resumeWithException(e)
            }
        }

        try {
            Terminal.getInstance().connectInternetReader(reader, connectionConfig, readerCallback)
        } catch (e: TerminalException) {
            Timber.e(e, "Failed to connect to reader")
            continuation.resumeWithException(e)
        }
    }

    suspend fun processPayment(paymentIntent: PaymentIntent) = suspendCoroutine<PaymentIntent> { continuation ->
        Terminal.getInstance().processPayment(paymentIntent,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Timber.d("Payment processed successfully")
                    continuation.resumeWith(Result.success(paymentIntent))
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Error processing payment")
                    continuation.resumeWithException(e)
                }
            }
        )
    }

    suspend fun collectPaymentMethod(paymentIntent: PaymentIntent) = suspendCoroutine<PaymentIntent> { continuation ->
        Terminal.getInstance().collectPaymentMethod(paymentIntent,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Timber.d("Payment method collected successfully")
                    continuation.resumeWith(Result.success(paymentIntent))
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Error collecting payment method")
                    continuation.resumeWithException(e)
                }
            }
        )
    }

    suspend fun retrievePaymentIntent(intent: ServerPaymentIntent) = suspendCoroutine<PaymentIntent> { continuation ->
        Terminal.getInstance().retrievePaymentIntent(intent.secret,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Timber.d("Payment intent retrieved successfully")
                    continuation.resumeWith(Result.success(paymentIntent))
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Error retrieving payment intent")
                    continuation.resumeWithException(e)
                }
            }
        )
    }
}