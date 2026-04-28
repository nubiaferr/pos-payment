package com.nubiaferr.pospayment.presentation.util

import androidx.annotation.StringRes
import com.nubiaferr.pospayment.R
import com.nubiaferr.pospayment.domain.exception.DebitLimitExceededException
import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
import com.nubiaferr.pospayment.domain.exception.PixLimitExceededException
import com.nubiaferr.pospayment.domain.exception.TransactionNotFoundException
import com.nubiaferr.pospayment.domain.exception.TransactionNotCancellableException
import com.nubiaferr.pospayment.domain.exception.VoucherLimitExceededException
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.validation.AmountValidationResult
import com.nubiaferr.pospayment.domain.validation.InstalmentsValidationResult

/**
 * Maps domain enums and typed validation results to Android string resource IDs.
 *
 * Centralizes all R.string references outside the ViewModel so the ViewModel
 * stays free of Android Context and is easily unit-testable.
 *
 * Usage in Fragment:
 * ```kotlin
 * binding.tvMethod.text = getString(transaction.method.labelRes())
 * binding.tilAmount.error = state.amountError?.toErrorString(requireContext())
 * ```
 */

@StringRes
fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CREDIT  -> R.string.payment_method_credit
    PaymentMethod.DEBIT   -> R.string.payment_method_debit
    PaymentMethod.PIX     -> R.string.payment_method_pix
    PaymentMethod.VOUCHER -> R.string.payment_method_voucher
}

@StringRes
fun TransactionStatus.labelRes(): Int = when (this) {
    TransactionStatus.APPROVED  -> R.string.status_approved
    TransactionStatus.DECLINED  -> R.string.status_declined
    TransactionStatus.CANCELLED -> R.string.status_cancelled
    TransactionStatus.PENDING   -> R.string.status_pending
}

/**
 * Resolves a typed [AmountValidationResult] to a user-facing error string.
 * Returns null for [AmountValidationResult.Valid].
 */
fun AmountValidationResult.toErrorString(context: android.content.Context): String? = when (this) {
    is AmountValidationResult.AmountZeroOrNegative ->
        context.getString(R.string.error_amount_required)
    is AmountValidationResult.ExceedsGlobalMax ->
        context.getString(R.string.error_amount_max, "%.2f".format(max))
    is AmountValidationResult.Valid -> null
}

/**
 * Resolves a typed [InstalmentsValidationResult] to a user-facing error string.
 * Returns null for [InstalmentsValidationResult.Valid].
 */
fun InstalmentsValidationResult.toErrorString(context: android.content.Context): String? = when (this) {
    is InstalmentsValidationResult.ExceedsMax ->
        context.getString(R.string.error_instalments_max, max)
    is InstalmentsValidationResult.Valid -> null
}

/**
 * Resolves any [Throwable] to a user-facing error message.
 * Business exceptions use typed string resources; unknown errors fall back to generic.
 */
fun Throwable.toErrorString(context: android.content.Context): String = when (this) {
    is InstalmentNotAllowedException ->
        context.getString(R.string.error_instalment_not_allowed, minAmount)
    is PixLimitExceededException ->
        context.getString(R.string.error_pix_limit_exceeded, limit)
    is DebitLimitExceededException ->
        context.getString(R.string.error_debit_limit_exceeded, limit)
    is VoucherLimitExceededException ->
        context.getString(R.string.error_voucher_limit_exceeded, limit)
    is TransactionNotCancellableException ->
        context.getString(R.string.error_transaction_not_cancellable, transactionId)
    is TransactionNotFoundException ->
        context.getString(R.string.error_transaction_not_found, transactionId)
    else -> message ?: context.getString(R.string.error_unknown)
}