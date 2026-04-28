package com.nubiaferr.pospayment.domain.strategy

import com.nubiaferr.pospayment.domain.exception.VoucherLimitExceededException
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

class VoucherPaymentStrategyTest {

    private lateinit var repository: PaymentRepository
    private lateinit var strategy: VoucherPaymentStrategy

    private val approvedTransaction = Transaction(
        id = "txn_voucher_001",
        payment = Payment(30.0, PaymentMethod.VOUCHER),
        status = TransactionStatus.APPROVED,
        authCode = "VCH_AUTH",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        repository = mockk()
        strategy = VoucherPaymentStrategy(repository)
    }

    @Test
    fun `given amount within limit, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 30.0, method = PaymentMethod.VOUCHER)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    @Test
    fun `given amount exactly at limit, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = VoucherPaymentStrategy.VOUCHER_MAX_AMOUNT, method = PaymentMethod.VOUCHER)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    @Test
    fun `given amount one cent above limit, when executed, then returns VoucherLimitExceededException`() = runTest {
        val payment = Payment(amount = VoucherPaymentStrategy.VOUCHER_MAX_AMOUNT + 0.01, method = PaymentMethod.VOUCHER)

        val result = strategy.execute(payment)

        assertTrue(result.exceptionOrNull() is VoucherLimitExceededException)
        coVerify(exactly = 0) { repository.processPayment(any()) }
    }

    @Test
    fun `given amount largely exceeding limit, when executed, then returns VoucherLimitExceededException`() = runTest {
        val payment = Payment(amount = 99_999.0, method = PaymentMethod.VOUCHER)

        val result = strategy.execute(payment)

        assertTrue(result.exceptionOrNull() is VoucherLimitExceededException)
    }

    @Test
    fun `given limit exceeded, when executed, then error message contains limit value`() = runTest {
        val payment = Payment(amount = 5_000.0, method = PaymentMethod.VOUCHER)

        val result = strategy.execute(payment)

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("1000") || message.contains("1.000"))
    }

    @Test
    fun `given repository returns failure, when executed, then propagates failure`() = runTest {
        val payment = Payment(amount = 30.0, method = PaymentMethod.VOUCHER)
        coEvery { repository.processPayment(payment) } returns Result.failure(Exception("Voucher network unavailable"))

        val result = strategy.execute(payment)

        assertEquals("Voucher network unavailable", result.exceptionOrNull()?.message)
    }
}