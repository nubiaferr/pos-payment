package com.nubiaferr.pospayment.data.mapper

import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PaymentDataMapper].
 *
 * Verifies correct conversion between data-layer DTOs and domain entities,
 * including amount unit conversion (cents ↔ BRL) and status mapping.
 */
class PaymentDataMapperTest {

    private lateinit var mapper: PaymentDataMapper

    @Before
    fun setUp() {
        mapper = PaymentDataMapper()
    }

    // ── toRequestDto ───────────────────────────────────────────────────────────

    @Test
    fun `given payment with BRL amount, when toRequestDto, then amount is converted to cents`() {
        val payment = Payment(amount = 99.99, method = PaymentMethod.CREDIT)

        val dto = mapper.toRequestDto(payment)

        assertEquals(9999L, dto.amount)
    }

    @Test
    fun `given credit payment, when toRequestDto, then method is lowercase credit`() {
        val payment = Payment(amount = 100.0, method = PaymentMethod.CREDIT)

        val dto = mapper.toRequestDto(payment)

        assertEquals("credit", dto.method)
    }

    @Test
    fun `given pix payment, when toRequestDto, then method is lowercase pix`() {
        val payment = Payment(amount = 100.0, method = PaymentMethod.PIX)

        val dto = mapper.toRequestDto(payment)

        assertEquals("pix", dto.method)
    }

    @Test
    fun `given payment with installments and description, when toRequestDto, then fields are preserved`() {
        val payment = Payment(
            amount = 150.0,
            method = PaymentMethod.CREDIT,
            installments = 3,
            description = "Office supplies"
        )

        val dto = mapper.toRequestDto(payment)

        assertEquals(3, dto.installments)
        assertEquals("Office supplies", dto.description)
    }

    // ── toDomain ──────────────────────────────────────────────────────────────

    @Test
    fun `given approved status dto, when toDomain, then transaction status is APPROVED`() {
        val dto = makeDto(status = "approved")
        val payment = Payment(100.0, PaymentMethod.CREDIT)

        val transaction = mapper.toDomain(dto, payment)

        assertEquals(TransactionStatus.APPROVED, transaction.status)
    }

    @Test
    fun `given declined status dto, when toDomain, then transaction status is DECLINED`() {
        val dto = makeDto(status = "declined")
        val payment = Payment(100.0, PaymentMethod.CREDIT)

        val transaction = mapper.toDomain(dto, payment)

        assertEquals(TransactionStatus.DECLINED, transaction.status)
    }

    @Test
    fun `given cancelled status dto, when toDomain, then transaction status is CANCELLED`() {
        val dto = makeDto(status = "cancelled")
        val payment = Payment(100.0, PaymentMethod.CREDIT)

        val transaction = mapper.toDomain(dto, payment)

        assertEquals(TransactionStatus.CANCELLED, transaction.status)
    }

    @Test
    fun `given unknown status dto, when toDomain, then transaction status is PENDING`() {
        val dto = makeDto(status = "processing")
        val payment = Payment(100.0, PaymentMethod.CREDIT)

        val transaction = mapper.toDomain(dto, payment)

        assertEquals(TransactionStatus.PENDING, transaction.status)
    }

    @Test
    fun `given dto with id and authCode, when toDomain, then transaction preserves both fields`() {
        val dto = makeDto(id = "txn_mapper_001", authCode = "AUTH_XYZ")
        val payment = Payment(100.0, PaymentMethod.DEBIT)

        val transaction = mapper.toDomain(dto, payment)

        assertEquals("txn_mapper_001", transaction.id)
        assertEquals("AUTH_XYZ", transaction.authCode)
    }

    @Test
    fun `given original payment, when toDomain, then transaction payment matches original`() {
        val dto = makeDto()
        val payment = Payment(amount = 75.0, method = PaymentMethod.VOUCHER, installments = 1)

        val transaction = mapper.toDomain(dto, payment)

        assertEquals(payment, transaction.payment)
    }

    @Test
    fun `given dto with uppercase status, when toDomain, then status is mapped correctly`() {
        val dto = makeDto(status = "APPROVED")
        val payment = Payment(100.0, PaymentMethod.PIX)

        val transaction = mapper.toDomain(dto, payment)

        assertEquals(TransactionStatus.APPROVED, transaction.status)
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private fun makeDto(
        id: String = "txn_001",
        status: String = "approved",
        authCode: String = "AUTH_TEST"
    ) = TransactionResponseDto(
        id = id,
        status = status,
        authCode = authCode,
        amount = 10_000L,
        method = "credit",
        installments = 1,
        description = "Test",
        timestamp = 1_700_000_000_000L
    )
}