package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Strategy for voucher payments (meal cards, food cards).
 *
 * Voucher payments follow the same flow as debit but are routed through a
 * separate acquirer network (e.g. VR, Alelo, Ticket). No additional business
 * rules are enforced at the domain level beyond what the acquirer validates.
 *
 * @property repository Data source for voucher payment processing.
 */
class VoucherPaymentStrategy @Inject constructor(
    private val repository: PaymentRepository
) : PaymentStrategy {

    override suspend fun execute(payment: Payment): Result<Transaction> {
        return repository.processVoucher(payment)
    }
}