package com.nubiaferr.pospayment.presentation.mapper

import android.content.Context
import com.nubiaferr.pospayment.R
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.presentation.model.TransactionUiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Maps domain [Transaction] entities into [TransactionUiModel] for display.
 *
 * All formatting is centralised here so Fragments and ViewModels stay free
 * of formatting logic. Localised labels are resolved from string resources.
 */
class PaymentUiMapper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun toUiModel(transaction: Transaction): TransactionUiModel {
        val payment = transaction.payment
        val instalmentsLabel = if (payment.installments > 1) {
            val instalment = currencyFormatter.format(payment.amount / payment.installments)
            context.getString(R.string.label_instalment_breakdown, payment.installments, instalment)
        } else {
            ""
        }

        return TransactionUiModel(
            id = transaction.id,
            formattedAmount = currencyFormatter.format(payment.amount),
            methodLabel = payment.method.toLabel(),
            instalments = instalmentsLabel,
            authCode = transaction.authCode,
            statusLabel = transaction.status.toLabel(),
            formattedDate = dateFormatter.format(Date(transaction.timestamp))
        )
    }

    private fun PaymentMethod.toLabel(): String = when (this) {
        PaymentMethod.CREDIT  -> context.getString(R.string.payment_method_credit)
        PaymentMethod.DEBIT   -> context.getString(R.string.payment_method_debit)
        PaymentMethod.PIX     -> context.getString(R.string.payment_method_pix)
        PaymentMethod.VOUCHER -> context.getString(R.string.payment_method_voucher)
    }

    private fun TransactionStatus.toLabel(): String = when (this) {
        TransactionStatus.APPROVED  -> context.getString(R.string.status_approved)
        TransactionStatus.DECLINED  -> context.getString(R.string.status_declined)
        TransactionStatus.CANCELLED -> context.getString(R.string.status_cancelled)
        TransactionStatus.PENDING   -> context.getString(R.string.status_pending)
    }
}