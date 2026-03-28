package com.nubiaferr.pospayment.domain.model

/**
 * Represents the result of a successfully processed payment on the POS terminal.
 *
 * Returned by the domain layer after a [Payment] is authorised. The presentation
 * layer maps this into a receipt-friendly UI model.
 *
 * @property id Unique transaction identifier returned by the acquirer.
 * @property payment The original [Payment] that originated this transaction.
 * @property status Current lifecycle status of the transaction.
 * @property authCode Authorization code returned by the acquirer.
 * @property timestamp Unix epoch (ms) at which the transaction was authorised.
 */
data class Transaction(
    val id: String,
    val payment: Payment,
    val status: TransactionStatus,
    val authCode: String,
    val timestamp: Long
)