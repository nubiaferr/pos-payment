package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.DebitLimitExceededException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Strategy for debit card payments.
 *
 * Business rule: single transaction cannot exceed R$ 10.000,00.
 */
class DebitPaymentStrategy @Inject constructor(
    private val repository: PaymentRepository
) : PaymentStrategy {

    override suspend fun execute(payment: Payment): Result<Transaction> {
        if (payment.amount > DEBIT_MAX_AMOUNT) {
            return Result.failure(DebitLimitExceededException(DEBIT_MAX_AMOUNT))
        }
        return repository.processPayment(payment)
    }

    companion object {
        const val DEBIT_MAX_AMOUNT = 10_000.0
    }
}