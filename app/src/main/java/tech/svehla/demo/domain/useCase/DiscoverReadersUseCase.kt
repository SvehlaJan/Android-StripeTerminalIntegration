package tech.svehla.demo.domain.useCase

import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.domain.model.ErrorReason

class DiscoverReadersUseCase(
    private val terminalClient: TerminalClient,
    private val errorMapper: ErrorMapper,
) {
    private val discoveryConfig = DiscoveryConfiguration(0, DiscoveryMethod.INTERNET, true)

    fun discoverReaders(): Flow<List<Reader>> {
        return terminalClient.discoverReaders(discoveryConfig)
            .catch { throw DiscoverReadersException(errorMapper.mapException(it)) }
            .flowOn(Dispatchers.IO)
    }
}

class DiscoverReadersException(val errorReason: ErrorReason) : Exception("Failed to discover readers")