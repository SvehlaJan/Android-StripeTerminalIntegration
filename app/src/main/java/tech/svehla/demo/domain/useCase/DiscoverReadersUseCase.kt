package tech.svehla.demo.domain.useCase

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.domain.model.ErrorReason
import timber.log.Timber
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class DiscoverReadersUseCase {
    private val discoveryConfig = DiscoveryConfiguration(0, DiscoveryMethod.INTERNET, true)

    fun discoverReaders() = callbackFlow<List<Reader>> {
        val discoveryCallback = object : Callback {
            override fun onSuccess() {
                Timber.d("Discovering readers")
            }

            override fun onFailure(e: TerminalException) {
                Timber.e(e, "Failed to discover readers")
                cancel(CancellationException("Terminal discovery failed", DiscoverReadersException(ErrorMapper.mapException(e))))
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
    }.flowOn(Dispatchers.IO)
}

class DiscoverReadersException(val errorReason: ErrorReason) : Exception("Failed to discover readers")