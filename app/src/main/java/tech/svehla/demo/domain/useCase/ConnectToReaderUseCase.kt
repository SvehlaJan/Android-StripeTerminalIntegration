package tech.svehla.demo.domain.useCase

import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.Reader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.svehla.demo.data.TerminalClient
import timber.log.Timber

class ConnectToReaderUseCase(
    private val terminalClient: TerminalClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun connect(reader: Reader) = withContext(dispatcher) {
        val connectionConfig = ConnectionConfiguration.InternetConnectionConfiguration(true)

        try {
            terminalClient.connectToReader(reader, connectionConfig)
        } catch (e: Exception) {
            Timber.e(e, "Error connecting to reader")
            throw e
        }
    }
}