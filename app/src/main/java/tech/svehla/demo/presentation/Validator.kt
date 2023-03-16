package tech.svehla.demo.presentation

import tech.svehla.demo.R

class Validator {
    fun validateAmount(amount: String): Int? {
        return if (amount.isEmpty()) {
            R.string.error_amount_empty
        } else if (amount.toDoubleOrNull() == null) {
            R.string.error_amount_invalid
        } else if (amount.toDouble() < 20.0) {
            R.string.error_amount_too_small
        } else if (amount.toDouble() > 50000.0) {
            R.string.error_amount_too_big
        } else {
            null
        }
    }

    fun validateReferenceNumber(referenceNumber: String): Int? {
        return if (referenceNumber.isEmpty()) {
            R.string.error_reference_empty
        } else if (referenceNumber.length > 20) {
            R.string.error_reference_invalid
        } else if (referenceNumber.indexOfFirst { !it.isDigit() } != -1) {
            R.string.error_reference_invalid
        } else {
            null
        }
    }

    fun convertAmount(amount: String): Int {
        return (amount.toDouble() * 100).toInt()
    }
}