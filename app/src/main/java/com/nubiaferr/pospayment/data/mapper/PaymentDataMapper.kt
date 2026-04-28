package com.nubiaferr.pospayment.data.mapper

import com.nubiaferr.pospayment.data.local.entity.TransactionEntity
import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import javax.inject.Inject

/**
 * Maps between data-layer DTOs/entities and domain-layer models.
 *
 * Isolates the domain from API and database contract changes.
 * If the acquirer renames a field, changes the amount unit, or a Room schema
 * migration alters a column, only this class needs to be updated.
 */
class PaymentDataMapper @Inject constructor() {

    /**
     * Converts a domain [Payment] into a [PaymentRequestDto] for the acquirer API.
     * Amount is converted from BRL (Double) to cents (Long) as required by the API contract.
     */
    fun toRequestDto(payment: Payment): PaymentRequestDto = PaymentRequestDto(
        amount = (payment.amount * 100).toLong(),
        method = payment.method.name.lowercase(),
        installments = payment.installments,
        description = payment.description
    )

    /**
     * Converts an API [TransactionResponseDto] into a domain [Transaction].
     *
     * @param dto             The response received from the acquirer.
     * @param originalPayment The original [Payment] that originated this transaction.
     *                        Preserved here because the API response does not include
     *                        all payment intent fields (e.g. description).
     */
    fun toDomain(dto: TransactionResponseDto, originalPayment: Payment): Transaction = Transaction(
        id = dto.id,
        payment = originalPayment,
        status = mapStatus(dto.status),
        authCode = dto.authCode,
        timestamp = dto.timestamp
    )

    /**
     * Converts a domain [Transaction] into a [TransactionEntity] for Room persistence.
     * Amount is stored as Long cents to avoid floating-point precision issues in SQLite.
     */
    fun toEntity(transaction: Transaction): TransactionEntity = TransactionEntity(
        id = transaction.id,
        amount = (transaction.payment.amount * 100).toLong(),
        paymentMethod = transaction.payment.method.name,
        installments = transaction.payment.installments,
        description = transaction.payment.description,
        status = transaction.status.name,
        authCode = transaction.authCode,
        timestamp = transaction.timestamp
    )

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun mapStatus(status: String): TransactionStatus = when (status.lowercase()) {
        "approved"  -> TransactionStatus.APPROVED
        "declined"  -> TransactionStatus.DECLINED
        "cancelled" -> TransactionStatus.CANCELLED
        else        -> TransactionStatus.PENDING
    }
}