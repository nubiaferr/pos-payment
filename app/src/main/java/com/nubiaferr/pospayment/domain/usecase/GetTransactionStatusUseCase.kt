package com.nubiaferr.pospayment.domain.usecase

import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Retrieves the current status of a transaction.
 *
 * Useful for polling pending transactions (e.g. Pix awaiting confirmation)
 * or for recovering from a terminal crash mid-transaction.
 * Currently unnecessary since the project uses a fake API, but it's here for scalability reasons.
 */
class GetTransactionStatusUseCase @Inject constructor(
    private val repository: PaymentRepository
) {
    suspend operator fun invoke(transactionId: String): Result<Transaction> =
        repository.getTransactionStatus(transactionId)
}