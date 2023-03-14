package tech.svehla.demo.domain.model

sealed class PaymentProgress {
    object PreparingPayment : PaymentProgress()
    object PaymentInProgress : PaymentProgress()
    object PaymentCompleted : PaymentProgress()
}
