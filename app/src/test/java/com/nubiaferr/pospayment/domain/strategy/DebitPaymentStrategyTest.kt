package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.DebitLimitExceededException
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
    fun `given amount within limit, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 500.0, method = PaymentMethod.DEBIT)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    @Test
    fun `given amount exactly at limit, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = DebitPaymentStrategy.DEBIT_MAX_AMOUNT, method = PaymentMethod.DEBIT)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given amount one cent above limit, when executed, then returns DebitLimitExceededException`() = runTest {
        val payment = Payment(amount = DebitPaymentStrategy.DEBIT_MAX_AMOUNT + 0.01, method = PaymentMethod.DEBIT)

        val result = strategy.execute(payment)

        assertTrue(result.exceptionOrNull() is DebitLimitExceededException)
        coVerify(exactly = 0) { repository.processPayment(any()) }
    }

    @Test
    fun `given amount largely exceeding limit, when executed, then returns DebitLimitExceededException`() = runTest {
        val payment = Payment(amount = 99_999.0, method = PaymentMethod.DEBIT)

        val result = strategy.execute(payment)

        assertTrue(result.exceptionOrNull() is DebitLimitExceededException)
    }

    @Test
    fun `given very small amount, when executed, then delegates without restriction`() = runTest {
        val payment = Payment(amount = 0.01, method = PaymentMethod.DEBIT)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `given repository returns failure, when executed, then propagates failure`() = runTest {
        val payment = Payment(amount = 50.0, method = PaymentMethod.DEBIT)
        coEvery { repository.processPayment(payment) } returns Result.failure(Exception("Card declined"))

        val result = strategy.execute(payment)

        assertEquals("Card declined", result.exceptionOrNull()?.message)
    }

    @Test
    fun `given any payment, when executed, then never calls any other repository method`() = runTest {
        val payment = Payment(amount = 50.0, method = PaymentMethod.DEBIT)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        strategy.execute(payment)

        coVerify(exactly = 1) { repository.processPayment(payment) }
    }
}