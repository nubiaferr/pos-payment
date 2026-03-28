package com.nubiaferr.pospayment.domain.usecase

import com.nubiaferr.pospayment.domain.exception.UnsupportedPaymentMethodException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.strategy.PaymentStrategy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProcessPaymentUseCase].
 *
 * Verifies that the correct strategy is selected for each [PaymentMethod],
 * that unsupported methods fail with [UnsupportedPaymentMethodException],
 * and that strategy failures propagate correctly.
 *
 * Each strategy's individual business rules are tested in their own test classes.
 */
class ProcessPaymentUseCaseTest {

    private lateinit var creditStrategy: PaymentStrategy
    private lateinit var debitStrategy: PaymentStrategy
    private lateinit var pixStrategy: PaymentStrategy
    private lateinit var voucherStrategy: PaymentStrategy
    private lateinit var useCase: ProcessPaymentUseCase

    private fun makeTransaction(method: PaymentMethod) = Transaction(
        id = "txn_${method.name.lowercase()}",
        payment = Payment(100.0, method),
        status = TransactionStatus.APPROVED,
        authCode = "AUTH_OK",
        timestamp = 1_700_000_000_000L
    )

    @Before
    fun setUp() {
        creditStrategy = mockk()
        debitStrategy = mockk()
        pixStrategy = mockk()
        voucherStrategy = mockk()

        useCase = ProcessPaymentUseCase(
            strategies = mapOf(
                PaymentMethod.CREDIT  to creditStrategy,
                PaymentMethod.DEBIT   to debitStrategy,
                PaymentMethod.PIX     to pixStrategy,
                PaymentMethod.VOUCHER to voucherStrategy,
            )
        )
    }

    // ── Strategy routing ───────────────────────────────────────────────────────

    @Test
    fun `given credit payment, when invoked, then routes to credit strategy only`() = runTest {
        val payment = Payment(100.0, PaymentMethod.CREDIT)
        coEvery { creditStrategy.execute(payment) } returns Result.success(makeTransaction(PaymentMethod.CREDIT))

        val result = useCase(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { creditStrategy.execute(payment) }
        coVerify(exactly = 0) { debitStrategy.execute(any()) }
        coVerify(exactly = 0) { pixStrategy.execute(any()) }
        coVerify(exactly = 0) { voucherStrategy.execute(any()) }
    }

    @Test
    fun `given debit payment, when invoked, then routes to debit strategy only`() = runTest {
        val payment = Payment(50.0, PaymentMethod.DEBIT)
        coEvery { debitStrategy.execute(payment) } returns Result.success(makeTransaction(PaymentMethod.DEBIT))

        val result = useCase(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { debitStrategy.execute(payment) }
        coVerify(exactly = 0) { creditStrategy.execute(any()) }
    }

    @Test
    fun `given pix payment, when invoked, then routes to pix strategy only`() = runTest {
        val payment = Payment(200.0, PaymentMethod.PIX)
        coEvery { pixStrategy.execute(payment) } returns Result.success(makeTransaction(PaymentMethod.PIX))

        val result = useCase(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { pixStrategy.execute(payment) }
        coVerify(exactly = 0) { creditStrategy.execute(any()) }
    }

    @Test
    fun `given voucher payment, when invoked, then routes to voucher strategy only`() = runTest {
        val payment = Payment(30.0, PaymentMethod.VOUCHER)
        coEvery { voucherStrategy.execute(payment) } returns Result.success(makeTransaction(PaymentMethod.VOUCHER))

        val result = useCase(payment)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { voucherStrategy.execute(payment) }
        coVerify(exactly = 0) { creditStrategy.execute(any()) }
    }

    // ── Unsupported method ─────────────────────────────────────────────────────

    @Test
    fun `given use case with no strategies registered, when invoked, then returns UnsupportedPaymentMethodException`() = runTest {
        val emptyUseCase = ProcessPaymentUseCase(strategies = emptyMap())
        val payment = Payment(100.0, PaymentMethod.CREDIT)

        val result = emptyUseCase(payment)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedPaymentMethodException)
    }

    @Test
    fun `given strategy map missing one method, when that method is used, then returns UnsupportedPaymentMethodException`() = runTest {
        val partialUseCase = ProcessPaymentUseCase(
            strategies = mapOf(PaymentMethod.CREDIT to creditStrategy)
        )
        val payment = Payment(100.0, PaymentMethod.PIX)

        val result = partialUseCase(payment)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedPaymentMethodException)
    }

    @Test
    fun `given unsupported method, when invoked, then error message contains method name`() = runTest {
        val emptyUseCase = ProcessPaymentUseCase(strategies = emptyMap())
        val payment = Payment(100.0, PaymentMethod.VOUCHER)

        val result = emptyUseCase(payment)

        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(message.contains("VOUCHER", ignoreCase = true))
    }

    // ── Failure propagation ────────────────────────────────────────────────────

    @Test
    fun `given strategy returns failure, when invoked, then propagates failure unchanged`() = runTest {
        val payment = Payment(100.0, PaymentMethod.CREDIT)
        val error = Exception("Terminal offline")
        coEvery { creditStrategy.execute(payment) } returns Result.failure(error)

        val result = useCase(payment)

        assertTrue(result.isFailure)
        assertEquals("Terminal offline", result.exceptionOrNull()?.message)
    }

    // ── Correct payment object forwarded ──────────────────────────────────────

    @Test
    fun `given payment with installments, when invoked, then forwards exact payment object to strategy`() = runTest {
        val payment = Payment(amount = 150.0, method = PaymentMethod.CREDIT, installments = 3, description = "Test")
        coEvery { creditStrategy.execute(payment) } returns Result.success(makeTransaction(PaymentMethod.CREDIT))

        useCase(payment)

        coVerify { creditStrategy.execute(payment) }
    }
}