package com.nubiaferr.pospayment.domain.repository

import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction

/**
 * Contract for payment data operations.
 *
 * The domain layer depends only on this interface. The concrete implementation
 * ([PaymentRepositoryImpl]) lives in the data layer
 * and is injected at runtime via Hilt.
 *
 * All functions are `suspend` for safe use within coroutines.
 */
interface PaymentRepository {

    /**
     * Processes a payment through the acquirer.
     *
     * The implementation routes to the correct endpoint based on [Payment.method].
     *
     * @param payment The payment intent, including amount, method and instalment count.
     * @return [Result.success] with the authorised [Transaction], or
     *         [Result.failure] wrapping a [BusinessException] for rule violations
     *         or an infrastructure [Exception] for network/SDK errors.
     */
    suspend fun processPayment(payment: Payment): Result<Transaction>

    /**
     * Cancels a previously approved transaction.
     *
     * @param transactionId The unique identifier of the transaction to cancel.
     * @return [Result.success] with the updated [Transaction], or [Result.failure].
     */
    suspend fun cancelTransaction(transactionId: String): Result<Transaction>

    /**
     * Retrieves the current status of a transaction by its identifier.
     * Falls back to local storage if the network is unavailable.
     *
     * @param transactionId The unique identifier of the transaction.
     * @return [Result.success] with the [Transaction], or [Result.failure].
     */
    suspend fun getTransactionStatus(transactionId: String): Result<Transaction>
}