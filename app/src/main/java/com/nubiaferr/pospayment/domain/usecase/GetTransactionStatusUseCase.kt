package com.nubiaferr.pospayment.domain.usecase

import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Retrieves the current status of a transaction.
 *
 * Useful for polling pending transactions (e.g. Pix awaiting confirmation)
 * or for recovering from a terminal crash mid-transaction.
 *
 * @property repository Contract for payment data operations.
 */
class GetTransactionStatusUseCase @Inject constructor(
    private val repository: PaymentRepository
) {

    /**
     * @param transactionId The unique identifier of the transaction to look up.
     * @return [Result.success] with the [Transaction], or [Result.failure].
     */
    suspend operator fun invoke(transactionId: String): Result<Transaction> {
        return repository.getTransactionStatus(transactionId)
    }
}