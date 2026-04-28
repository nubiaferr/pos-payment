package com.nubiaferr.pospayment.domain.exception

/**
 * Base class for all domain-level business rule violations.
 *
 * Messages are intentionally kept in English here because the domain layer
 * has no Android context. Localised, user-facing strings are produced in
 * the presentation layer (PaymentUiMapper / PaymentViewModel) using
 * string resources.
 */
sealed class BusinessException(message: String) : Exception(message)

class UnsupportedPaymentMethodException(
    val method: String
) : BusinessException("Payment method '$method' is not supported on this terminal.")

class InstalmentNotAllowedException(
    val minAmount: Double
) : BusinessException("Instalment payments require a minimum amount of R$${"%.2f".format(minAmount)}.")

class PixLimitExceededException(
    val limit: Double
) : BusinessException("Pix transactions are limited to R$${"%.2f".format(limit)} per transaction.")

class DebitLimitExceededException(
    val limit: Double
) : BusinessException("Debit transactions are limited to R$${"%.2f".format(limit)} per transaction.")

class VoucherLimitExceededException(
    val limit: Double
) : BusinessException("Voucher transactions are limited to R$${"%.2f".format(limit)} per transaction.")

class TransactionNotCancellableException(
    val transactionId: String
) : BusinessException("Transaction '$transactionId' cannot be cancelled in its current state.")