package com.nubiaferr.pospayment.data.repository

import com.nubiaferr.pospayment.data.local.dao.PaymentDao
import com.nubiaferr.pospayment.data.local.entity.TransactionEntity
import com.nubiaferr.pospayment.data.mapper.PaymentDataMapper
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import com.nubiaferr.pospayment.data.remote.service.PaymentService
import com.nubiaferr.pospayment.domain.exception.TransactionNotCancellableException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaymentRepositoryImplTest {

    private lateinit var service: PaymentService
    private lateinit var dao: PaymentDao
    private lateinit var mapper: PaymentDataMapper
    private lateinit var repository: PaymentRepositoryImpl

    private val payment = Payment(amount = 100.0, method = PaymentMethod.CREDIT)

    private val responseDto = TransactionResponseDto(
        id = "txn_001",
        status = "approved",
        authCode = "AUTH",
        amount = 10_000L,
        method = "credit",
        installments = 1,
        description = "",
        timestamp = 1_700_000_000_000L
    )

    private val approvedEntity = TransactionEntity(
        id = "txn_001",
        amount = 10_000L,
        paymentMethod = "CREDIT",
        installments = 1,
        description = "",
        status = "APPROVED",
        authCode = "AUTH",
        timestamp = 1_700_000_000_000L
    )

    private val approvedTransaction = Transaction(
        id = "txn_001",
        payment = payment,
        status = TransactionStatus.APPROVED,
        authCode = "AUTH",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        service = mockk()
        dao = mockk(relaxed = true)
        mapper = mockk()
        repository = PaymentRepositoryImpl(service, dao, mapper)
    }

    // ── processPayment ─────────────────────────────────────────────────────────

    @Test
    fun `given service success, when processPayment, then returns transaction`() = runTest {
        coEvery { mapper.toRequestDto(payment) } returns mockk()
        coEvery { service.processPayment(any()) } returns Result.success(responseDto)
        coEvery { mapper.toDomain(responseDto, payment) } returns approvedTransaction
        coEvery { mapper.toEntity(approvedTransaction) } returns approvedEntity

        val result = repository.processPayment(payment)

        assertTrue(result.isSuccess)
        assertEquals(approvedTransaction, result.getOrNull())
    }

    @Test
    fun `given service success, when processPayment, then caches transaction locally`() = runTest {
        coEvery { mapper.toRequestDto(payment) } returns mockk()
        coEvery { service.processPayment(any()) } returns Result.success(responseDto)
        coEvery { mapper.toDomain(responseDto, payment) } returns approvedTransaction
        coEvery { mapper.toEntity(approvedTransaction) } returns approvedEntity

        repository.processPayment(payment)

        coVerify(exactly = 1) { dao.upsert(approvedEntity) }
    }

    @Test
    fun `given service failure, when processPayment, then does not cache`() = runTest {
        coEvery { mapper.toRequestDto(payment) } returns mockk()
        coEvery { service.processPayment(any()) } returns Result.failure(Exception("Timeout"))

        val result = repository.processPayment(payment)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    // ── cancelTransaction ──────────────────────────────────────────────────────

    @Test
    fun `given transaction not APPROVED, when cancelTransaction, then returns TransactionNotCancellableException`() = runTest {
        coEvery { dao.getById("txn_001") } returns approvedEntity.copy(status = "PENDING")

        val result = repository.cancelTransaction("txn_001")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is TransactionNotCancellableException)
        coVerify(exactly = 0) { service.cancelTransaction(any()) }
    }

    @Test
    fun `given approved transaction, when cancelTransaction, then calls service and caches result`() = runTest {
        val cancelDto = responseDto.copy(status = "cancelled")
        val cancelledTx = approvedTransaction.copy(status = TransactionStatus.CANCELLED)
        val cancelledEntity = approvedEntity.copy(status = "CANCELLED")

        coEvery { dao.getById("txn_001") } returns approvedEntity
        coEvery { service.cancelTransaction("txn_001") } returns Result.success(cancelDto)
        coEvery { mapper.toDomain(cancelDto, any()) } returns cancelledTx
        coEvery { mapper.toEntity(cancelledTx) } returns cancelledEntity

        val result = repository.cancelTransaction("txn_001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { service.cancelTransaction("txn_001") }
        coVerify(exactly = 1) { dao.upsert(cancelledEntity) }
    }

    @Test
    fun `given transaction not found locally, when cancelTransaction, then returns failure`() = runTest {
        coEvery { dao.getById(any()) } returns null
        coEvery { service.cancelTransaction(any()) } returns Result.success(responseDto)

        val result = repository.cancelTransaction("txn_not_found")

        assertTrue(result.isFailure)
    }

    // ── getTransactionStatus — offline fallback ────────────────────────────────

    @Test
    fun `given network available, when getTransactionStatus, then fetches from service and updates cache`() = runTest {
        coEvery { service.getTransaction("txn_001") } returns Result.success(responseDto)
        coEvery { dao.getById("txn_001") } returns approvedEntity
        coEvery { mapper.toDomain(responseDto, any()) } returns approvedTransaction
        coEvery { mapper.toEntity(approvedTransaction) } returns approvedEntity

        val result = repository.getTransactionStatus("txn_001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dao.upsert(approvedEntity) }
    }

    @Test
    fun `given network unavailable and cache exists, when getTransactionStatus, then returns cached transaction`() = runTest {
        coEvery { service.getTransaction(any()) } returns Result.failure(Exception("No network"))
        coEvery { dao.getById("txn_001") } returns approvedEntity

        val result = repository.getTransactionStatus("txn_001")

        assertTrue(result.isSuccess)
        assertEquals(TransactionStatus.APPROVED, result.getOrNull()?.status)
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `given network unavailable and no cache, when getTransactionStatus, then returns failure`() = runTest {
        coEvery { service.getTransaction(any()) } returns Result.failure(Exception("No network"))
        coEvery { dao.getById(any()) } returns null

        val result = repository.getTransactionStatus("txn_not_found")

        assertTrue(result.isFailure)
    }
}