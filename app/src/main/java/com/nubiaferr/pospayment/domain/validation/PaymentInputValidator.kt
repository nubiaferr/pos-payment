package com.nubiaferr.pospayment.domain.validation

import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.strategy.DebitPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.PixPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.VoucherPaymentStrategy
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Validates payment input values before they reach the use case.
 *
 * Returns typed [AmountValidationResult] and [InstalmentsValidationResult]
 * instead of raw strings. The presentation layer resolves error messages
 * from string resources using the typed result.
 */
class PaymentInputValidator @Inject constructor() {

    fun validateAmount(
        amount: Double,
        method: PaymentMethod? = null
    ): AmountValidationResult {
        val value = BigDecimal(amount.toString())

        if (value <= BigDecimal.ZERO) {
            return AmountValidationResult.AmountZeroOrNegative
        }

        if (value > MAX_TRANSACTION_AMOUNT) {
            return AmountValidationResult.ExceedsGlobalMax(MAX_TRANSACTION_AMOUNT.toDouble())
        }

        val methodLimit = method?.maxAmount()
        if (methodLimit != null && value > BigDecimal(methodLimit.toString())) {
            return AmountValidationResult.ExceedsMethodLimit(
                method = method,
                limit = methodLimit
            )
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

private fun PaymentMethod.maxAmount(): Double? = when (this) {
    PaymentMethod.DEBIT   -> DebitPaymentStrategy.DEBIT_MAX_AMOUNT
    PaymentMethod.PIX     -> PixPaymentStrategy.PIX_MAX_AMOUNT
    PaymentMethod.VOUCHER -> VoucherPaymentStrategy.VOUCHER_MAX_AMOUNT
    PaymentMethod.CREDIT  -> null
}

sealed class AmountValidationResult {
    data class Valid(val amount: Double) : AmountValidationResult()
    object AmountZeroOrNegative : AmountValidationResult()
    data class ExceedsGlobalMax(val max: Double) : AmountValidationResult()
    data class ExceedsMethodLimit(val method: PaymentMethod, val limit: Double) : AmountValidationResult()
}

sealed class InstalmentsValidationResult {
    data class Valid(val installments: Int) : InstalmentsValidationResult()
    data class ExceedsMax(val max: Int) : InstalmentsValidationResult()
}