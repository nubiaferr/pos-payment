package com.nubiaferr.pospayment.domain.model

/**
 * Represents the supported payment methods available on the POS terminal.
 *
 * Each method maps to a concrete [PaymentStrategy]
 * that encapsulates its specific business rules.
 */
enum class PaymentMethod {
    CREDIT,
    DEBIT,
    PIX,
    VOUCHER
}