package com.nubiaferr.pospayment.presentation.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * UI-friendly representation of a completed transaction.
 *
 * All values are pre-formatted strings ready for direct display —
 * no formatting logic belongs in the Fragment.
 *
 * @property id             Transaction identifier shown on the receipt.
 * @property formattedAmount Amount formatted as "R$ 99,99".
 * @property methodLabel    Human-readable payment method (e.g. "Crédito", "Pix").
 * @property instalments    Instalment description (e.g. "3x de R$ 33,33") or empty.
 * @property authCode       Authorization code for the receipt.
 * @property statusLabel    Localised status string (e.g. "Aprovado").
 * @property formattedDate  Date/time formatted as "dd/MM/yyyy HH:mm".
 */
@Parcelize
data class TransactionUiModel(
    val id: String,
    val formattedAmount: String,
    val methodLabel: String,
    val instalments: String,
    val authCode: String,
    val statusLabel: String,
    val formattedDate: String
): Parcelable