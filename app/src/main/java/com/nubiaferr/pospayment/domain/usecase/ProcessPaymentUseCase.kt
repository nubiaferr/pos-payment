package com.nubiaferr.pospayment.domain.usecase

import com.nubiaferr.pospayment.domain.exception.UnsupportedPaymentMethodException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.strategy.PaymentStrategy
import javax.inject.Inject

/**
 * Processes a payment by delegating to the appropriate [PaymentStrategy].
 *
 * This use case is the single entry point for all payment processing in the domain.
 * It selects the correct strategy from the injected map at runtime, keeping the
 * use case itself free of any `when`/`if` branching on [PaymentMethod].
 *
 * Adding a new payment method requires only:
 * 1. A new [PaymentMethod] enum value.
 * 2. A new [PaymentStrategy] implementation.
 * 3. Registering it in [PaymentModule].
 *
 * No changes to this class (Open/Closed Principle).
 *
 * @property strategies Map of [PaymentMethod] to its corresponding [PaymentStrategy],
 *                      assembled and injected by Hilt.
 */
class ProcessPaymentUseCase @Inject constructor(
    private val strategies: Map<PaymentMethod, @JvmSuppressWildcards PaymentStrategy>
) {

    /**
     * Executes the payment processing flow.
     *
     * @param payment The [Payment] intent to process.
     * @return [Result.success] with the authorised [Transaction], or
     *         [Result.failure] with a [BusinessException]
     *         for rule violations or an infrastructure [Exception] for network/SDK errors.
     */
    suspend operator fun invoke(payment: Payment): Result<Transaction> {
        val strategy = strategies[payment.method]
            ?: return Result.failure(
                UnsupportedPaymentMethodException(payment.method.name)
            )
        return strategy.execute(payment)
    }
}