package com.nubiaferr.pospayment.presentation.mapper

import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.presentation.model.TransactionUiModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Maps domain [Transaction] entities into [TransactionUiModel] for display.
 *
 * All formatting is centralised here so Fragments and ViewModels stay free
 * of `String.format`, `SimpleDateFormat`, and locale concerns.
 */
class PaymentUiMapper @Inject constructor() {

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    /**
     * Converts a domain [Transaction] into a display-ready [TransactionUiModel].
     *
     * @param transaction The authorised transaction returned by the domain layer.
     * @return A [TransactionUiModel] with all values pre-formatted for the UI.
     */
    fun toUiModel(transaction: Transaction): TransactionUiModel {
        val payment = transaction.payment
        val instalmentsLabel = if (payment.installments > 1) {
            val instalment = currencyFormatter.format(payment.amount / payment.installments)
            "${payment.installments}x de $instalment"
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
        PaymentMethod.CREDIT -> "Crédito"
        PaymentMethod.DEBIT -> "Débito"
        PaymentMethod.PIX -> "Pix"
        PaymentMethod.VOUCHER -> "Voucher"
    }

    private fun TransactionStatus.toLabel(): String = when (this) {
        TransactionStatus.APPROVED -> "Aprovado"
        TransactionStatus.DECLINED -> "Negado"
        TransactionStatus.CANCELLED -> "Cancelado"
        TransactionStatus.PENDING -> "Pendente"
    }
}