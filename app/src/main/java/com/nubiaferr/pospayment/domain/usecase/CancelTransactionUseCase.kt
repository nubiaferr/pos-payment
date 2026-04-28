package com.nubiaferr.pospayment.domain.usecase

import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Cancels a previously approved transaction.
 *
 * Delegates validation and reversal to [PaymentRepository], which is responsible
 * for checking the transaction status before attempting cancellation.
 */
class CancelTransactionUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(transactionId: String): Result<Transaction> =
        repository.cancelTransaction(transactionId)
}