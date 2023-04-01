package tech.svehla.demo.domain.useCase

import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import tech.svehla.demo.data.TerminalClient

class DiscoverReadersUseCase(
    private val terminalClient: TerminalClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val discoveryConfig = DiscoveryConfiguration(0, DiscoveryMethod.INTERNET, true)

    fun discoverReaders(): Flow<List<Reader>> {
        return terminalClient.discoverReaders(discoveryConfig)
            .flowOn(dispatcher)
    }
}