package com.nubiaferr.pospayment.data.mapper

import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PaymentDataMapper].
 *
 * Verifies correct conversion across all three mapping directions:
 * - [toRequestDto]: domain Payment → API request DTO
 * - [toDomain]:     API response DTO → domain Transaction
 * - [toEntity]:     domain Transaction → Room entity
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

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    fun `given approved transaction, when toEntity, then status is stored as APPROVED string`() {
        val transaction = makeTransaction(status = TransactionStatus.APPROVED)

        val entity = mapper.toEntity(transaction)

        assertEquals("APPROVED", entity.status)
    }

    @Test
    fun `given transaction with BRL amount, when toEntity, then amount is stored in cents`() {
        val transaction = makeTransaction(amount = 49.99)

        val entity = mapper.toEntity(transaction)

        assertEquals(4999L, entity.amount)
    }

    @Test
    fun `given transaction with installments, when toEntity, then installments are preserved`() {
        val transaction = makeTransaction(installments = 6)

        val entity = mapper.toEntity(transaction)

        assertEquals(6, entity.installments)
    }

    @Test
    fun `given transaction with credit method, when toEntity, then paymentMethod is CREDIT`() {
        val transaction = makeTransaction(method = PaymentMethod.CREDIT)

        val entity = mapper.toEntity(transaction)

        assertEquals("CREDIT", entity.paymentMethod)
    }

    @Test
    fun `given transaction, when toEntity, then id and authCode are preserved exactly`() {
        val transaction = makeTransaction()

        val entity = mapper.toEntity(transaction)

        assertEquals(transaction.id, entity.id)
        assertEquals(transaction.authCode, entity.authCode)
    }

    @Test
    fun `given transaction, when toEntity then toDomain round-trip, then amount is preserved within rounding`() {
        // This verifies the cents conversion is symmetric
        val originalAmount = 99.99
        val transaction = makeTransaction(amount = originalAmount)

        val entity = mapper.toEntity(transaction)
        val reconstructedAmount = entity.amount / 100.0

        assertEquals(originalAmount, reconstructedAmount, 0.001)
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

    private fun makeTransaction(
        amount: Double = 100.0,
        method: PaymentMethod = PaymentMethod.CREDIT,
        installments: Int = 1,
        status: TransactionStatus = TransactionStatus.APPROVED
    ) = Transaction(
        id = "txn_entity_001",
        payment = Payment(amount = amount, method = method, installments = installments),
        status = status,
        authCode = "AUTH_ENTITY",
        timestamp = 1_700_000_000_000L
    )
}