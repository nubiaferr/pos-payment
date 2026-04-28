package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.VoucherLimitExceededException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Strategy for voucher payments (meal/food cards).
 *
 * Business rule: single transaction cannot exceed [VOUCHER_MAX_AMOUNT].
 * Voucher networks typically enforce lower limits than card networks.
 */
class VoucherPaymentStrategy @Inject constructor(
    private val repository: PaymentRepository
) : PaymentStrategy {

    override suspend fun execute(payment: Payment): Result<Transaction> {
        if (payment.amount > VOUCHER_MAX_AMOUNT) {
            return Result.failure(VoucherLimitExceededException(VOUCHER_MAX_AMOUNT))
        }
        return repository.processPayment(payment)
    }

    companion object {
        const val VOUCHER_MAX_AMOUNT = 1_000.0
    }
}