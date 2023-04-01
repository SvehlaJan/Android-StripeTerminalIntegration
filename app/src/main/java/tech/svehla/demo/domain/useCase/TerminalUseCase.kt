package tech.svehla.demo.domain.useCase

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.svehla.demo.data.TerminalClient
import timber.log.Timber

class TerminalUseCase(
    private val terminalClient: TerminalClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun initTerminal(applicationContext: Context) = withContext(dispatcher) {
        try {
            terminalClient.initTerminal(applicationContext)
        } catch (e: Exception) {
            Timber.e("Error while initializing Stripe SDK: $e")
            throw e
        }
    }

    fun observeConnectionStatus() = terminalClient.connectionStatus

    fun observePaymentStatus() = terminalClient.paymentStatus

    fun observeConnectedReader() = terminalClient.connectedReader
}