package com.nubiaferr.pospayment.domain.strategy

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
 * Unit tests for [DebitPaymentStrategy].
 *
 * Debit has no extra business rules — verifies that every payment
 * is delegated directly to the repository and failures propagate correctly.
 */
class DebitPaymentStrategyTest {

    private lateinit var repository: PaymentRepository
    private lateinit var strategy: DebitPaymentStrategy

    private val approvedTransaction = Transaction(
        id = "txn_debit_001",
        payment = Payment(50.0, PaymentMethod.DEBIT),
        status = TransactionStatus.APPROVED,
        authCode = "DEBIT_AUTH",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        repository = mockk()
        strategy = DebitPaymentStrategy(repository)
    }

    @Test
    fun `given any valid amount, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 50.0, method = PaymentMethod.DEBIT)
        coEvery { repository.processDebit(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processDebit(payment) }
    }

    @Test
    fun `given very small amount, when executed, then delegates without restriction`() = runTest {
        val payment = Payment(amount = 0.01, method = PaymentMethod.DEBIT)
        coEvery { repository.processDebit(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given large amount, when executed, then delegates without restriction`() = runTest {
        val payment = Payment(amount = 999_999.0, method = PaymentMethod.DEBIT)
        coEvery { repository.processDebit(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given repository returns failure, when executed, then propagates failure`() = runTest {
        val payment = Payment(amount = 50.0, method = PaymentMethod.DEBIT)
        coEvery { repository.processDebit(payment) } returns Result.failure(Exception("Card declined"))

        val result = strategy.execute(payment)

        assertTrue(result.isFailure)
        assertEquals("Card declined", result.exceptionOrNull()?.message)
    }

    @Test
    fun `given any payment, when executed, then never calls any other repository method`() = runTest {
        val payment = Payment(amount = 50.0, method = PaymentMethod.DEBIT)
        coEvery { repository.processDebit(payment) } returns Result.success(approvedTransaction)

        strategy.execute(payment)

        coVerify(exactly = 0) { repository.processCredit(any()) }
        coVerify(exactly = 0) { repository.processPix(any()) }
        coVerify(exactly = 0) { repository.processVoucher(any()) }
    }
}