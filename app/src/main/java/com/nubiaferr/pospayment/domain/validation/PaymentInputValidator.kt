package com.nubiaferr.pospayment.domain.validation

import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.strategy.CreditPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.DebitPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.PixPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.VoucherPaymentStrategy
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Validates payment input values before they reach the use case.
 *
 * Amount validation is aware of the selected [PaymentMethod] so the
 * per-method limit error can be shown in real time while the operator types,
 * without waiting for the strategy to reject the transaction after submit.
 *
 * Pure Kotlin — no Android dependencies. KMP-ready for commonMain.
 */
class PaymentInputValidator @Inject constructor() {

    /**
     * Validates the amount for a given payment method.
     *
     * Checks in order:
     * 1. Must be greater than zero
     * 2. Must not exceed the global terminal maximum
     * 3. Must not exceed the per-method limit
     *
     * @param amount The pre-parsed value from [MoneyFormatter].
     * @param method The currently selected payment method, or null if none selected yet.
     */
    fun validateAmount(
        amount: Double,
        method: PaymentMethod? = null
    ): AmountValidationResult {
        val value = BigDecimal(amount.toString())

        if (value <= BigDecimal.ZERO) {
            return AmountValidationResult.Invalid("O valor deve ser maior que zero")
        }

        if (value > MAX_TRANSACTION_AMOUNT) {
            return AmountValidationResult.Invalid(
                "Valor máximo por transação: R\$ ${MAX_TRANSACTION_AMOUNT.toPlainString()}"
            )
        }

        val methodLimit = method?.maxAmount()
        if (methodLimit != null && value > BigDecimal(methodLimit.toString())) {
            return AmountValidationResult.Invalid(
                "Valor máximo para ${method.label()}: R\$ ${"%.2f".format(methodLimit).replace(".", ",")}"
            )
        }

        return AmountValidationResult.Valid(amount)
    }

    fun validateInstallments(raw: String): InstalmentsValidationResult {
        val parsed = raw.trim().toIntOrNull()
            ?: return InstalmentsValidationResult.Valid(1)

        if (parsed < 1) return InstalmentsValidationResult.Valid(1)

        if (parsed > MAX_INSTALLMENTS) {
            return InstalmentsValidationResult.Invalid(
                "Máximo de $MAX_INSTALLMENTS parcelas permitidas"
            )
        }

        return InstalmentsValidationResult.Valid(parsed)
    }

    companion object {
        val MAX_TRANSACTION_AMOUNT: BigDecimal = BigDecimal("99999.99")
        const val MAX_INSTALLMENTS: Int = 12
    }
}

/** Returns the per-method transaction limit, or null if the method has no specific limit. */
private fun PaymentMethod.maxAmount(): Double? = when (this) {
    PaymentMethod.DEBIT   -> DebitPaymentStrategy.DEBIT_MAX_AMOUNT
    PaymentMethod.PIX     -> PixPaymentStrategy.PIX_MAX_AMOUNT
    PaymentMethod.VOUCHER -> VoucherPaymentStrategy.VOUCHER_MAX_AMOUNT
    PaymentMethod.CREDIT  -> null  // Credit limit is enforced by instalment rules, not amount
}

private fun PaymentMethod.label(): String = when (this) {
    PaymentMethod.CREDIT  -> "Crédito"
    PaymentMethod.DEBIT   -> "Débito"
    PaymentMethod.PIX     -> "Pix"
    PaymentMethod.VOUCHER -> "Voucher"
}

sealed class AmountValidationResult {
    data class Valid(val amount: Double) : AmountValidationResult()
    data class Invalid(val message: String) : AmountValidationResult()
}

sealed class InstalmentsValidationResult {
    data class Valid(val installments: Int) : InstalmentsValidationResult()
    data class Invalid(val message: String) : InstalmentsValidationResult()
}