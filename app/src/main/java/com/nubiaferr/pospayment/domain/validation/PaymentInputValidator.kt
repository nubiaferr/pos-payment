package com.nubiaferr.pospayment.domain.validation

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

/**
 * Validates raw user input before it reaches the payment use case.
 *
 * Keeps formatting and boundary rules out of both the Fragment and the ViewModel.
 * Pure Kotlin — no Android dependencies, fully unit-testable.
 *
 * KMP note: can be moved to commonMain as-is.
 */
class PaymentInputValidator @Inject constructor() {

    /**
     * Validates and parses the raw amount string typed by the operator.
     *
     * Accepts both dot and comma as decimal separators (e.g. "29.90" or "29,90").
     *
     * @return [AmountValidationResult.Valid] with the parsed value,
     *         or [AmountValidationResult.Invalid] with a user-facing message.
     */
    fun validateAmount(raw: String): AmountValidationResult {
        val trimmed = raw.trim()

        if (trimmed.isBlank()) {
            return AmountValidationResult.Invalid("Informe um valor")
        }

        val normalised = trimmed.replace(",", ".")
        val amount = try {
            BigDecimal(normalised).setScale(2, RoundingMode.HALF_UP)
        } catch (e: NumberFormatException) {
            return AmountValidationResult.Invalid("Informe um valor válido (ex: 29,90)")
        }

        if (amount <= BigDecimal.ZERO) {
            return AmountValidationResult.Invalid("O valor deve ser maior que zero")
        }

        if (amount > MAX_TRANSACTION_AMOUNT) {
            return AmountValidationResult.Invalid(
                "Valor máximo por transação: R\$ ${MAX_TRANSACTION_AMOUNT.toPlainString()}"
            )
        }

        return AmountValidationResult.Valid(amount.toDouble())
    }

    /**
     * Validates the instalment count entered by the operator.
     *
     * @return A sanitised instalment count, clamped to [1, MAX_INSTALLMENTS].
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