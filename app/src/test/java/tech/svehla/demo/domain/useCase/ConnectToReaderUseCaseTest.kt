package tech.svehla.demo.domain.useCase

import com.google.common.truth.Truth
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class ConnectToReaderUseCaseTest {

    @MockK
    private lateinit var terminalClient: TerminalClient

    @MockK
    private lateinit var errorMapper: ErrorMapper

    private lateinit var useCase: ConnectToReaderUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        useCase = ConnectToReaderUseCase(terminalClient, errorMapper)
    }

    @Test
    fun `On invoke should connect to reader`() = runTest {
        coEvery { terminalClient.connectToReader(any(), any()) } just Runs

        useCase(mockk())

        coVerify { terminalClient.connectToReader(any(), any()) }
    }

    @Test
    fun `On invoke should wrap exception and rethrow if exception is thrown from terminal client`() = runTest {
        coEvery { terminalClient.connectToReader(any(), any()) } throws Exception()
        coEvery { errorMapper.mapException(any()) } returns mockk()

        val throwable = try {
            useCase(mockk())
            null
        } catch (t: Throwable) {
            t
        }

        coVerify { errorMapper.mapException(any()) }
        Truth.assertThat(throwable).isInstanceOf(ConnectToReaderException::class.java)
    }
}