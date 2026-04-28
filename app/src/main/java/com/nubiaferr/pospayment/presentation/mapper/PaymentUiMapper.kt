package com.nubiaferr.pospayment.presentation.mapper

import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.presentation.model.TransactionUiModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Maps domain [Transaction] entities into [TransactionUiModel] for display.
 *
 * Responsible for data formatting (currency, date, instalment breakdown).
 */
class PaymentUiMapper @Inject constructor() {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun toUiModel(
        transaction: Transaction,
        methodLabel: String,
        statusLabel: String
    ): TransactionUiModel {
        val payment = transaction.payment
        val instalmentsLabel = if (payment.installments > 1) {
            formatInstalmentSummary(
                instalments = payment.installments,
                amountPerInstalment = payment.amount / payment.installments
            )
        } else {
            ""
        }

        return TransactionUiModel(
            id = transaction.id,
            formattedAmount = currencyFormatter.format(payment.amount),
            methodLabel = methodLabel,
            instalments = instalmentsLabel,
            authCode = transaction.authCode,
            statusLabel = statusLabel,
            formattedDate = dateFormatter.format(Date(transaction.timestamp))
        )
    }

    /**
     * Formats a per-instalment breakdown string for real-time display.
     * e.g. formatInstalmentSummary(12, 50.0) → "12x of R$ 50,00"
     *
     * Reuses [currencyFormatter] so the live preview and receipt are always consistent.
     * The caller is responsible for applying the [R.string.label_instalment_breakdown]
     * format string with the returned count and formatted amount.
     */
    fun formatInstalmentSummary(instalments: Int, amountPerInstalment: Double): String {
        val formatted = currencyFormatter.format(amountPerInstalment)
        return "${instalments}x $formatted"
    }
}