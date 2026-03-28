package com.nubiaferr.pospayment.data.mapper

import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import javax.inject.Inject

/**
 * Maps between data-layer DTOs and domain-layer entities.
 *
 * Isolates the domain from API contract changes. If the acquirer renames a field
 * or changes the amount unit, only this class is updated.
 */
class PaymentDataMapper @Inject constructor() {

    /**
     * Converts a domain [Payment] into a [PaymentRequestDto] suitable for the API.
     *
     * Amount is converted from BRL (Double) to cents (Long) as required by the API.
     *
     * @param payment The domain payment entity.
     * @return The corresponding API request DTO.
     */
    fun toRequestDto(payment: Payment): PaymentRequestDto = PaymentRequestDto(
        amount = (payment.amount * 100).toLong(),
        method = payment.method.name.lowercase(),
        installments = payment.installments,
        description = payment.description
    )

    /**
     * Converts a [TransactionResponseDto] received from the API into a domain [Transaction].
     *
     * @param dto The API response DTO.
     * @param originalPayment The original [Payment] that triggered this transaction,
     *                        used to reconstruct the domain entity.
     * @return The corresponding domain transaction entity.
     */
    fun toDomain(dto: TransactionResponseDto, originalPayment: Payment): Transaction = Transaction(
        id = dto.id,
        payment = originalPayment,
        status = mapStatus(dto.status),
        authCode = dto.authCode,
        timestamp = dto.timestamp
    )

    private fun mapStatus(status: String): TransactionStatus = when (status.lowercase()) {
        "approved" -> TransactionStatus.APPROVED
        "declined" -> TransactionStatus.DECLINED
        "cancelled" -> TransactionStatus.CANCELLED
        else -> TransactionStatus.PENDING
    }

    private fun mapMethod(method: String): PaymentMethod = when (method.lowercase()) {
        "credit" -> PaymentMethod.CREDIT
        "debit" -> PaymentMethod.DEBIT
        "pix" -> PaymentMethod.PIX
        "voucher" -> PaymentMethod.VOUCHER
        else -> throw IllegalArgumentException("Unknown payment method from API: $method")
    }
}