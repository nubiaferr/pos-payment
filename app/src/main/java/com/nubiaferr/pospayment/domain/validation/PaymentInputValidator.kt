package com.nubiaferr.pospayment.domain.validation

import java.math.BigDecimal
import javax.inject.Inject

/**
 * Validates payment input values before they reach the use case.
 *
 * Amount arrives as a pre-parsed [Double] from [MoneyTextWatcher] — this class
 * only enforces business boundary rules (zero, negative, maximum).
 * Instalment count is validated and clamped here as well.
 */
class PaymentInputValidator @Inject constructor() {

    /**
     * Validates a pre-parsed amount value.
     *
     * @param amount The value already parsed by [MoneyTextWatcher].
     * @return [AmountValidationResult.Valid] or [AmountValidationResult.Invalid].
     */
    fun validateAmount(amount: Double): AmountValidationResult {
        val value = BigDecimal(amount.toString())

        if (value <= BigDecimal.ZERO) {
            return AmountValidationResult.Invalid("O valor deve ser maior que zero")
        }

        if (value > MAX_TRANSACTION_AMOUNT) {
            return AmountValidationResult.Invalid(
                "Valor máximo por transação: R\$ ${MAX_TRANSACTION_AMOUNT.toPlainString()}"
            )
        }

        return AmountValidationResult.Valid(amount)
    }

    /**
     * Sanitises the instalment count entered by the operator.
     *
     * @return A value clamped to [1, MAX_INSTALLMENTS]. Defaults to 1 for blank/invalid input.
     */
    fun validateInstallments(raw: String): Int =
        raw.trim().toIntOrNull()?.coerceIn(1, MAX_INSTALLMENTS) ?: 1

    companion object {
        val MAX_TRANSACTION_AMOUNT: BigDecimal = BigDecimal("99999.99")
        const val MAX_INSTALLMENTS: Int = 12
    }
}

/** Result of [PaymentInputValidator.validateAmount]. */
sealed class AmountValidationResult {
    data class Valid(val amount: Double) : AmountValidationResult()
    data class Invalid(val message: String) : AmountValidationResult()
}