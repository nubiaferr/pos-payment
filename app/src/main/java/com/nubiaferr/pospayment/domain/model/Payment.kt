package com.nubiaferr.pospayment.domain.model

/**
 * Represents a payment intent initiated by the merchant on the POS terminal.
 *
 * This is a pure domain entity — it carries only business-relevant data and
 * has no dependency on any framework, SDK, or infrastructure detail.
 *
 * @property amount The total amount to be charged, in BRL.
 * @property method The payment method selected by the customer.
 * @property installments Number of installments. Only applicable for [PaymentMethod.CREDIT].
 * @property description Optional merchant description for the transaction.
 */
data class Payment(
    val amount: Double,
    val method: PaymentMethod,
    val installments: Int = 1,
    val description: String = ""
)