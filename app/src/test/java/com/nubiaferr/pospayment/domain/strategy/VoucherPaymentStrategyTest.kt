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
 * Unit tests for [VoucherPaymentStrategy].
 *
 * Voucher has no extra business rules beyond what the acquirer network validates.
 * Tests verify direct repository delegation and failure propagation.
 */
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
    fun `given valid payment, when executed, then delegates to repository`() = runTest {
        val payment = Payment(amount = 30.0, method = PaymentMethod.VOUCHER)
        coEvery { repository.processPayment(payment) } returns Result.success(approvedTransaction)

        val result = strategy.execute(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { repository.processPayment(payment) }
    }

    @Test
    fun `given repository returns failure, when executed, then propagates failure`() = runTest {
        val payment = Payment(amount = 30.0, method = PaymentMethod.VOUCHER)
        coEvery { repository.processPayment(payment) } returns Result.failure(Exception("Voucher network unavailable"))

        val result = strategy.execute(payment)

        assertTrue(result.isFailure)
        assertEquals("Voucher network unavailable", result.exceptionOrNull()?.message)
    }
}