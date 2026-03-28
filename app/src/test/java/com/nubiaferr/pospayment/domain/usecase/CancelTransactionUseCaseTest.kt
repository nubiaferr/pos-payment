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
 * Unit tests for [CancelTransactionUseCase].
 */
class CancelTransactionUseCaseTest {

    private lateinit var repository: PaymentRepository
    private lateinit var useCase: CancelTransactionUseCase

    private val cancelledTransaction = Transaction(
        id = "txn_cancel_001",
        payment = Payment(100.0, PaymentMethod.CREDIT),
        status = TransactionStatus.CANCELLED,
        authCode = "CANCEL_AUTH",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CancelTransactionUseCase(repository)
    }

    @Test
    fun `given valid transaction id, when invoked, then delegates to repository`() = runTest {
        coEvery { repository.cancelTransaction("txn_cancel_001") } returns Result.success(cancelledTransaction)

        val result = useCase("txn_cancel_001")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.cancelTransaction("txn_cancel_001") }
    }

    @Test
    fun `given valid transaction id, when invoked, then returns cancelled transaction`() = runTest {
        coEvery { repository.cancelTransaction("txn_cancel_001") } returns Result.success(cancelledTransaction)

        val result = useCase("txn_cancel_001")

        assertEquals(TransactionStatus.CANCELLED, result.getOrNull()?.status)
    }

    @Test
    fun `given repository returns failure, when invoked, then propagates failure`() = runTest {
        coEvery { repository.cancelTransaction(any()) } returns Result.failure(Exception("Cannot cancel"))

        val result = useCase("txn_cancel_001")

        assertTrue(result.isFailure)
        assertEquals("Cannot cancel", result.exceptionOrNull()?.message)
    }
}