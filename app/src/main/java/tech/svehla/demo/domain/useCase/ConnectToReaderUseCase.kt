package tech.svehla.demo.domain.useCase

import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.domain.model.ErrorReason
import timber.log.Timber

class ConnectToReaderUseCase(
    private val terminalClient: TerminalClient,
    private val errorMapper: ErrorMapper,
) {
    suspend operator fun invoke(reader: Reader) = withContext(Dispatchers.IO) {
        val connectionConfig = ConnectionConfiguration.InternetConnectionConfiguration(true)

        try {
            terminalClient.connectToReader(reader, connectionConfig)
        } catch (e: TerminalException) {
            Timber.e(e, "Error connecting to reader")
            throw ConnectToReaderException(errorMapper.mapException(e))
        }
    }
}

class ConnectToReaderException(val errorReason: ErrorReason) : Exception("Failed to connect to reader")