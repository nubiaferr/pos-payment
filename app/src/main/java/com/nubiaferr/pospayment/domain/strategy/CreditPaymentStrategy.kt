package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Strategy for credit card payments.
 *
 * Business rules enforced:
 * - Instalment plans (> 1x) require a minimum transaction amount of R$10.00.
 *
 * @property repository Data source for credit payment processing.
 */
class CreditPaymentStrategy @Inject constructor(
    private val repository: PaymentRepository
) : PaymentStrategy {

    override suspend fun execute(payment: Payment): Result<Transaction> {
        if (payment.installments > 1 && payment.amount < MIN_INSTALMENT_AMOUNT) {
            return Result.failure(InstalmentNotAllowedException(MIN_INSTALMENT_AMOUNT))
        }
        return repository.processCredit(payment)
    }

    companion object {
        /** Minimum amount in BRL required to enable instalment payments. */
        const val MIN_INSTALMENT_AMOUNT = 10.0
    }
}