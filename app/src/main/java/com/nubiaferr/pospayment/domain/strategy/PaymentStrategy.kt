package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction

/**
* Strategy interface for payment processing.
*
* Each payment method ([PaymentMethod]) has a dedicated
* implementation that encapsulates its specific business rules and validation logic.
*
* The Strategy pattern allows [ProcessPaymentUseCase] to
* select the correct behaviour at runtime without any `when` or `if` branching on the
* payment method — new methods are added by creating a new strategy and registering it
* in the DI module, without touching existing code (Open/Closed Principle).
*
* Implementations:
* - [CreditPaymentStrategy]
* - [DebitPaymentStrategy]
* - [PixPaymentStrategy]
* - [VoucherPaymentStrategy]
*/
interface PaymentStrategy {

    /**
     * Validates business rules and delegates the payment to the repository.
     *
     * @param payment The payment intent to process.
     * @return [Result.Success] wrapping the authorised [Transaction], or
     *         [Result.Failure] wrapping a [BusinessException]
     *         for rule violations, or any other [Exception] for infrastructure errors.
     */
    suspend fun execute(payment: Payment): Result<Transaction>
}
