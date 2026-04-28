package com.nubiaferr.pospayment.domain.validation

import java.math.BigDecimal
import javax.inject.Inject

/**
 * Validates raw payment input values before they reach the use case.
 *
 * Responsibility is limited to format and global boundary rules that are
 * method-agnostic (zero, negative, global maximum, instalment count).
 *
 * Per-method transaction limits (e.g. Pix R$ 50k, Debit R$ 10k) are
 * enforced exclusively by the corresponding [PaymentStrategy] implementation,
 * which is the single source of truth for those rules.
 *
 * Returns typed results instead of raw strings — the presentation layer
 * resolves error messages from string resources.
 */
class PaymentInputValidator @Inject constructor() {

    fun validateAmount(amount: Double): AmountValidationResult {
        val value = BigDecimal(amount.toString())

        if (value <= BigDecimal.ZERO) {
            return AmountValidationResult.AmountZeroOrNegative
        }

        if (value > MAX_TRANSACTION_AMOUNT) {
            return AmountValidationResult.ExceedsGlobalMax(MAX_TRANSACTION_AMOUNT.toDouble())
        }

        return AmountValidationResult.Valid(amount)
    }

    fun validateInstallments(raw: String): InstalmentsValidationResult {
        val parsed = raw.trim().toIntOrNull()
            ?: return InstalmentsValidationResult.Valid(1)

        if (parsed < 1) return InstalmentsValidationResult.Valid(1)

        if (parsed > MAX_INSTALLMENTS) {
            return InstalmentsValidationResult.ExceedsMax(MAX_INSTALLMENTS)
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
    object AmountZeroOrNegative : AmountValidationResult()
    data class ExceedsGlobalMax(val max: Double) : AmountValidationResult()
}

sealed class InstalmentsValidationResult {
    data class Valid(val installments: Int) : InstalmentsValidationResult()
    data class ExceedsMax(val max: Int) : InstalmentsValidationResult()
}