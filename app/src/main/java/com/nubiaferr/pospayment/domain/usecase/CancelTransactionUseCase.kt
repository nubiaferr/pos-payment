package com.nubiaferr.pospayment.domain.usecase

import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Cancels a previously approved transaction.
 *
 * Delegates validation and reversal to [PaymentRepository]. The repository
 * implementation is responsible for checking the transaction status before
 * attempting cancellation.
 *
 * @property repository Contract for payment data operations.
 */
class CancelTransactionUseCase @Inject constructor(
    private val repository: PaymentRepository
) {

    /**
     * @param transactionId The unique identifier of the transaction to cancel.
     * @return [Result.success] with the updated [Transaction], or [Result.failure].
     */
    suspend operator fun invoke(transactionId: String): Result<Transaction> {
        return repository.cancelTransaction(transactionId)
    }
}
