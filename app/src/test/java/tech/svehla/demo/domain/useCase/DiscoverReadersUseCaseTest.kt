package tech.svehla.demo.domain.useCase

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.stripe.stripeterminal.external.models.Reader
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.svehla.demo.api.ErrorMapper
import tech.svehla.demo.data.TerminalClient
import tech.svehla.demo.testUtil.MainCoroutineExtension

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MainCoroutineExtension::class)
class DiscoverReadersUseCaseTest {

    @MockK
    private lateinit var terminalClient: TerminalClient

    @MockK
    private lateinit var errorMapper: ErrorMapper

    private lateinit var useCase: DiscoverReadersUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        useCase = DiscoverReadersUseCase(terminalClient, errorMapper)
    }

    @Test
    fun `On discoverReaders should return flow with readers`() = runTest {
        val readers = listOf(mockk<Reader>())
        val readersFlow: Flow<List<Reader>> = flow { emit(readers) }
        coEvery { terminalClient.discoverReaders(any()) } returns readersFlow

        val result = useCase.discoverReaders()
        advanceUntilIdle()

        result.test {
            Truth.assertThat(awaitItem()).isEqualTo(readers)
            awaitComplete()
        }
    }

    @Test
    fun `On discoverReaders should wrap exception and rethrow if exception is thrown from terminal client`() = runTest {
        val readersFlow: Flow<List<Reader>> = flow { throw Exception() }
        coEvery { terminalClient.discoverReaders(any()) } returns readersFlow
        coEvery { errorMapper.mapException(any()) } returns mockk()

        val result = useCase.discoverReaders()
        advanceUntilIdle()

        result.test {
            val exception = awaitError()
            Truth.assertThat(exception).isInstanceOf(DiscoverReadersException::class.java)
            coVerify { errorMapper.mapException(any()) }
        }
    }
}