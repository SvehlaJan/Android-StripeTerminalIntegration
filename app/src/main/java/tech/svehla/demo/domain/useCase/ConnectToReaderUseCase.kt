package tech.svehla.demo.domain.useCase

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.domain.model.ErrorReason
import timber.log.Timber
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class ConnectToReaderUseCase {
    suspend operator fun invoke(reader: Reader) = withContext(Dispatchers.IO) {
        suspendCoroutine<Boolean> { continuation ->
            val connectionConfig = ConnectionConfiguration.InternetConnectionConfiguration(true)

            val readerCallback = object : ReaderCallback {
                override fun onSuccess(reader: Reader) {
                    Timber.d("Connected to reader")
                    continuation.resumeWith(Result.success(true))
                }

                override fun onFailure(e: TerminalException) {
                    Timber.e(e, "Failed to connect to reader")
                    continuation.resumeWithException(ConnectToReaderException(ErrorMapper.mapException(e)))
                }
            }

            try {
                Terminal.getInstance().connectInternetReader(reader, connectionConfig, readerCallback)
            } catch (e: TerminalException) {
                Timber.e(e, "Failed to connect to reader")
                continuation.resumeWithException(ConnectToReaderException(ErrorMapper.mapException(e)))
            }
        }
    }
}

class ConnectToReaderException(val errorReason: ErrorReason) : Exception("Failed to connect to reader")