package tech.svehla.demo.presentation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tech.svehla.demo.R
import java.util.stream.Stream

class ValidatorTest {

    companion object {
        @JvmStatic
        private fun provideAmountValidationData(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("", R.string.error_amount_empty),
                Arguments.of("abc", R.string.error_amount_invalid),
                Arguments.of("19.99", R.string.error_amount_too_small),
                Arguments.of("50000.01", R.string.error_amount_too_big),
                Arguments.of("100.00", null)
            )
        }

        @JvmStatic
        private fun provideReferenceNumberValidationData(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("", R.string.error_reference_empty),
                Arguments.of("123456789012345678901", R.string.error_reference_invalid),
                Arguments.of("1234567890123456789a", R.string.error_reference_invalid),
                Arguments.of("12345678901234567890", null)
            )
        }

        @JvmStatic
        private fun provideAmountConversionData(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("1.00", 100),
                Arguments.of("1", 100),
                Arguments.of("1.1", 110),
                Arguments.of("1.01", 101),
                Arguments.of("1.11", 111),
                Arguments.of("1.111", 111),
                Arguments.of("0", 0),
                Arguments.of("0.0", 0),
                Arguments.of("0.1", 10),
                Arguments.of("0.01", 1),
                Arguments.of("0.001", 0),
            )
        }
    }

    private val validator = Validator()

    @ParameterizedTest
    @MethodSource("provideAmountValidationData")
    fun `validateAmount should return correct error message`(amount: String, expected: Int?) {
        val actual = validator.validateAmount(amount)
        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @MethodSource("provideReferenceNumberValidationData")
    fun `validateReferenceNumber should return correct error message`(referenceNumber: String, expected: Int?) {
        val actual = validator.validateReferenceNumber(referenceNumber)
        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @MethodSource("provideAmountConversionData")
    fun `convertAmount should return correct amount`(amount: String, expected: Int) {
        val actual = validator.convertAmount(amount)
        assertEquals(expected, actual)
    }
}