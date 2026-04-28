package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.PixLimitExceededException
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

class PixPaymentStrategyTest {

    private lateinit var repository: PaymentRepository
    private lateinit var strategy: PixPaymentStrategy

    private val approvedTransaction = Transaction(
        id = "txn_pix_001",
        payment = Payment(1_000.0, PaymentMethod.PIX),
        status = TransactionStatus.APPROVED,
        authCode = "PIX_AUTH",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        repository = mockk()
        strategy = PixPaymentStrategy(repository)
    }

    @Test
    fun `given amount well within limit, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 1_000.0, method = PaymentMethod.PIX)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    @Test
    fun `given very small amount, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 0.01, method = PaymentMethod.PIX)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given amount exactly at limit, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = PixPaymentStrategy.PIX_MAX_AMOUNT, method = PaymentMethod.PIX)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    @Test
    fun `given amount one cent above limit, when executed, then returns PixLimitExceededException`() = runTest {
        val payment = Payment(amount = PixPaymentStrategy.PIX_MAX_AMOUNT + 0.01, method = PaymentMethod.PIX)

        val result = strategy.execute(payment)

        assertTrue(result.exceptionOrNull() is PixLimitExceededException)
        coVerify(exactly = 0) { repository.processPayment(any()) }
    }

    @Test
    fun `given amount largely exceeding limit, when executed, then returns PixLimitExceededException`() = runTest {
        val payment = Payment(amount = 100_000.0, method = PaymentMethod.PIX)

        val result = strategy.execute(payment)

        assertTrue(result.exceptionOrNull() is PixLimitExceededException)
    }

    @Test
    fun `given limit exceeded, when executed, then error message contains limit value`() = runTest {
        val payment = Payment(amount = 60_000.0, method = PaymentMethod.PIX)

        val result = strategy.execute(payment)

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("50000") || message.contains("50.000"))
    }

    @Test
    fun `given valid amount and repository failure, when executed, then propagates failure`() = runTest {
        val payment = Payment(amount = 500.0, method = PaymentMethod.PIX)
        coEvery { repository.processPayment(payment) } returns Result.failure(Exception("Pix network error"))

        val result = strategy.execute(payment)

        assertEquals("Pix network error", result.exceptionOrNull()?.message)
    }
}