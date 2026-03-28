package com.nubiaferr.pospayment.domain.usecase

import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [GetTransactionStatusUseCase].
 */
class GetTransactionStatusUseCaseTest {

    private lateinit var repository: PaymentRepository
    private lateinit var useCase: GetTransactionStatusUseCase

    private val pendingTransaction = Transaction(
        id = "txn_status_001",
        payment = Payment(200.0, PaymentMethod.PIX),
        status = TransactionStatus.PENDING,
        authCode = "",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetTransactionStatusUseCase(repository)
    }

    @Test
    fun `given valid transaction id, when invoked, then delegates to repository`() = runTest {
        coEvery { repository.getTransactionStatus("txn_status_001") } returns Result.success(pendingTransaction)

        val result = useCase("txn_status_001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.getTransactionStatus("txn_status_001") }
    }

    @Test
    fun `given valid transaction id, when invoked, then returns transaction with correct status`() = runTest {
        coEvery { repository.getTransactionStatus("txn_status_001") } returns Result.success(pendingTransaction)

        val result = useCase("txn_status_001")

        assertEquals(TransactionStatus.PENDING, result.getOrNull()?.status)
    }

    @Test
    fun `given repository failure, when invoked, then propagates failure`() = runTest {
        coEvery { repository.getTransactionStatus(any()) } returns Result.failure(Exception("Not found"))

        val result = useCase("txn_status_001")

        assertTrue(result.isFailure)
        assertEquals("Not found", result.exceptionOrNull()?.message)
    }
}