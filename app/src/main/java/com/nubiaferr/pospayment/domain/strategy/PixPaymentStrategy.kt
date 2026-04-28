package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.PixLimitExceededException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Strategy for Pix instant payments.
 *
 * Business rule: single transaction cannot exceed [PIX_MAX_AMOUNT].
 */
class PixPaymentStrategy @Inject constructor(
    private val repository: PaymentRepository
) : PaymentStrategy {

    override suspend fun execute(payment: Payment): Result<Transaction> {
        if (payment.amount > PIX_MAX_AMOUNT) {
            return Result.failure(PixLimitExceededException(PIX_MAX_AMOUNT))
        }
        return repository.processPayment(payment)
    }

    companion object {
        const val PIX_MAX_AMOUNT = 50_000.0
    }
}