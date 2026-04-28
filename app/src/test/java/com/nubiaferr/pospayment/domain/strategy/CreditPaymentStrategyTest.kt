package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
* Unit tests for [CreditPaymentStrategy].
*
* Covers every branch of the instalment business rule and verifies
* that the repository is called only when all rules pass.
*/
class CreditPaymentStrategyTest {

    private lateinit var repository: PaymentRepository
    private lateinit var strategy: CreditPaymentStrategy

    private val approvedTransaction = Transaction(
        id = "txn_credit_001",
        payment = Payment(100.0, PaymentMethod.CREDIT),
        status = TransactionStatus.APPROVED,
        authCode = "AUTH123",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        repository = mockk()
        strategy = CreditPaymentStrategy(repository)
    }

    // ── Single instalment ──────────────────────────────────────────────────────

    @Test
    fun `given single instalment and any amount, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 5.0, method = PaymentMethod.CREDIT, installments = 1)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    // ── Instalment boundary — below minimum ────────────────────────────────────

    @Test
    fun `given 2 instalments and amount below minimum, when executed, then returns InstalmentNotAllowedException`() = runTest {
        val payment = Payment(amount = 9.99, method = PaymentMethod.CREDIT, installments = 2)

        val result = strategy.execute(payment)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InstalmentNotAllowedException)
        coVerify(exactly = 0) { repository.processPayment(any()) }
    }

    @Test
    fun `given many instalments and amount just below minimum, when executed, then returns InstalmentNotAllowedException`() = runTest {
        val payment = Payment(amount = 9.99, method = PaymentMethod.CREDIT, installments = 12)

        val result = strategy.execute(payment)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InstalmentNotAllowedException)
    }

    // ── Instalment boundary — exactly at minimum ───────────────────────────────

    @Test
    fun `given 2 instalments and amount exactly at minimum, when executed, then delegates to repository`() = runTest {
        val payment = Payment(
            amount = CreditPaymentStrategy.MIN_INSTALMENT_AMOUNT,
            method = PaymentMethod.CREDIT,
            installments = 2
        )
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    // ── Instalment boundary — above minimum ───────────────────────────────────

    @Test
    fun `given 3 instalments and amount above minimum, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 150.0, method = PaymentMethod.CREDIT, installments = 3)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    // ── Repository failure propagation ─────────────────────────────────────────

    @Test
    fun `given valid payment and repository returns failure, when executed, then propagates failure`() = runTest {
        val payment = Payment(amount = 100.0, method = PaymentMethod.CREDIT)
        val exception = Exception("Acquirer timeout")
        coEvery { repository.processPayment(payment) } returns Result.failure(exception)

        val result = strategy.execute(payment)

        assertTrue(result.isFailure)
        assertEquals("Acquirer timeout", result.exceptionOrNull()?.message)
    }

    // ── Error message content ──────────────────────────────────────────────────

    @Test
    fun `given instalment violation, when executed, then error message contains minimum amount`() = runTest {
        val payment = Payment(amount = 5.0, method = PaymentMethod.CREDIT, installments = 2)

        val result = strategy.execute(payment)

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("10"))
    }

    // ── Rule does not apply to single instalment ───────────────────────────────

    @Test
    fun `given 1 instalment and amount below minimum, when executed, then does not fail`() = runTest {
        val payment = Payment(amount = 1.0, method = PaymentMethod.CREDIT, installments = 1)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertFalse(result.isFailure)
    }
}