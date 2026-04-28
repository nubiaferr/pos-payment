package com.nubiaferr.pospayment.domain.validation

import java.math.BigDecimal
import javax.inject.Inject

/**
 * Validates payment input values before they reach the use case.
 *
 * Amount arrives as a pre-parsed [Double] from [MoneyFormatter].
 * Instalment count is validated and returns a typed result so the
 * presentation layer can show specific errors per field.
 */
class PaymentInputValidator @Inject constructor() {

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
     * Validates the instalment count.
     *
     * Returns [InstalmentsValidationResult.Invalid] when the input exceeds
     * [MAX_INSTALLMENTS] so the caller can show an explicit field error
     * instead of silently clamping the value.
     *
     * @param raw Raw string from the instalment input field.
     * @return [InstalmentsValidationResult.Valid] with the parsed value,
     *         or [InstalmentsValidationResult.Invalid] with a user-facing message.
     */
    fun validateInstallments(raw: String): InstalmentsValidationResult {
        val parsed = raw.trim().toIntOrNull()
            ?: return InstalmentsValidationResult.Valid(1)   // blank/empty → default 1x

        if (parsed < 1) {
            return InstalmentsValidationResult.Valid(1)
        }

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

sealed class AmountValidationResult {
    data class Valid(val amount: Double) : AmountValidationResult()
    data class Invalid(val message: String) : AmountValidationResult()
}

sealed class InstalmentsValidationResult {
    data class Valid(val installments: Int) : InstalmentsValidationResult()
    data class Invalid(val message: String) : InstalmentsValidationResult()
}