package com.nubiaferr.pospayment.domain.exception

/**
 * Base class for all domain-level business rule violations.
 *
 * Subclass this for each distinct business constraint so the presentation layer
 * can handle them individually without inspecting message strings.
 *
 * @param message Human-readable description of the rule that was violated.
 */
sealed class BusinessException(message: String) : Exception(message)

/**
 * Thrown when the selected [PaymentMethod] has no registered [PaymentStrategy].
 */
class UnsupportedPaymentMethodException(
    method: String
) : BusinessException("Payment method '$method' is not supported on this terminal.")

/**
 * Thrown when an instalment plan is requested on a payment that does not meet
 * the minimum amount threshold.
 *
 * @param minAmount The minimum amount required for instalment payments.
 */
class InstalmentNotAllowedException(
    minAmount: Double
) : BusinessException("Instalment payments require a minimum amount of R$${"%.2f".format(minAmount)}.")

/**
 * Thrown when a Pix transfer exceeds the acquirer's single-transaction limit.
 *
 * @param limit The maximum amount allowed for a single Pix transaction.
 */
class PixLimitExceededException(
    limit: Double
) : BusinessException("Pix transactions are limited to R$${"%.2f".format(limit)} per transaction.")

/**
 * Thrown when a cancellation is requested for a transaction that is not in an APPROVED state.
 */
class TransactionNotCancellableException(
    transactionId: String
) : BusinessException("Transaction '$transactionId' cannot be cancelled in its current state.")