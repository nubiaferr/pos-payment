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
 * All functions are `suspend` to allow callers to run them inside a coroutine
 * without blocking the main thread.
 */
interface PaymentRepository {

    /**
     * Processes a credit card payment through the acquirer.
     *
     * @param payment The payment intent, including amount and instalment count.
     * @return [Result.success] with the authorised [Transaction], or
     *         [Result.failure] wrapping a [com.btg.pos.domain.exception.BusinessException]
     *         or a network/SDK-level [Exception].
     */
    suspend fun processCredit(payment: Payment): Result<Transaction>

    /**
     * Processes a debit card payment through the acquirer.
     *
     * @param payment The payment intent.
     * @return [Result.success] with the authorised [Transaction], or [Result.failure].
     */
    suspend fun processDebit(payment: Payment): Result<Transaction>

    /**
     * Initiates a Pix QR-code payment and waits for acquirer confirmation.
     *
     * @param payment The payment intent.
     * @return [Result.success] with the authorised [Transaction], or [Result.failure].
     */
    suspend fun processPix(payment: Payment): Result<Transaction>

    /**
     * Processes a voucher (meal/food-card) payment through the acquirer.
     *
     * @param payment The payment intent.
     * @return [Result.success] with the authorised [Transaction], or [Result.failure].
     */
    suspend fun processVoucher(payment: Payment): Result<Transaction>

    /**
     * Cancels a previously approved transaction.
     *
     * @param transactionId The unique identifier of the transaction to cancel.
     * @return [Result.success] with the updated [Transaction], or [Result.failure].
     */
    suspend fun cancelTransaction(transactionId: String): Result<Transaction>

    /**
     * Retrieves the current status of a transaction by its identifier.
     *
     * Falls back to local storage if the network is unavailable.
     *
     * @param transactionId The unique identifier of the transaction.
     * @return [Result.success] with the [Transaction], or [Result.failure].
     */
    suspend fun getTransactionStatus(transactionId: String): Result<Transaction>
}