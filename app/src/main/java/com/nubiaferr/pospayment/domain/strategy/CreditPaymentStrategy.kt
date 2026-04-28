package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Strategy for credit card payments.
 *
 * Business rule: instalment plans (> 1x) require a minimum of R$ 10,00.
 */
class CreditPaymentStrategy @Inject constructor(
    private val repository: PaymentRepository
) : PaymentStrategy {

    override suspend fun execute(payment: Payment): Result<Transaction> {
        if (payment.installments > 1 && payment.amount < MIN_INSTALMENT_AMOUNT) {
            return Result.failure(InstalmentNotAllowedException(MIN_INSTALMENT_AMOUNT))
        }
        return repository.processPayment(payment)
    }

    companion object {
        const val MIN_INSTALMENT_AMOUNT = 10.0
    }
}