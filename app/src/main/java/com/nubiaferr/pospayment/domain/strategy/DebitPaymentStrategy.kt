package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Strategy for debit card payments.
 *
 * Debit payments have no specific instalment rules. The acquirer handles
 * all card-network validation (PIN, balance check) transparently.
 *
 * @property repository Data source for debit payment processing.
 */
class DebitPaymentStrategy @Inject constructor(
    private val repository: PaymentRepository
) : PaymentStrategy {

    override suspend fun execute(payment: Payment): Result<Transaction> {
        return repository.processPayment(payment)
    }
}