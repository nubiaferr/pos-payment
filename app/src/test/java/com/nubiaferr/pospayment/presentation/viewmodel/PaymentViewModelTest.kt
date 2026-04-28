package com.nubiaferr.pospayment.presentation.viewmodel

import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.usecase.CancelTransactionUseCase
import com.nubiaferr.pospayment.domain.usecase.ProcessPaymentUseCase
import com.nubiaferr.pospayment.domain.validation.AmountValidationResult
import com.nubiaferr.pospayment.domain.validation.InstalmentsValidationResult
import com.nubiaferr.pospayment.domain.validation.PaymentInputValidator
import com.nubiaferr.pospayment.presentation.mapper.PaymentUiMapper
import com.nubiaferr.pospayment.presentation.model.TransactionUiModel
import com.nubiaferr.pospayment.presentation.uistate.PaymentUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var processPaymentUseCase: ProcessPaymentUseCase
    private lateinit var cancelTransactionUseCase: CancelTransactionUseCase
    private lateinit var mapper: PaymentUiMapper
    private lateinit var validator: PaymentInputValidator
    private lateinit var viewModel: PaymentViewModel

    private val approvedTransaction = Transaction(
        id = "txn_001",
        payment = Payment(100.0, PaymentMethod.CREDIT),
        status = TransactionStatus.APPROVED,
        authCode = "AUTH",
        timestamp = 1_700_000_000_000L
    )

    private val uiModel = TransactionUiModel(
        id = "txn_001",
        formattedAmount = "R$ 100,00",
        methodLabel = "Credit",
        instalments = "",
        authCode = "AUTH",
        statusLabel = "Approved",
        formattedDate = "01/01/2025 12:00"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        processPaymentUseCase = mockk()
        cancelTransactionUseCase = mockk()
        mapper = mockk(relaxed = true)
        validator = mockk()

        // Default answers — tests override when they need specific behaviour
        every { validator.validateAmount(any()) } answers {
            val amount = firstArg<Double>()
            if (amount > 0.0) AmountValidationResult.Valid(amount)
            else AmountValidationResult.AmountZeroOrNegative
        }
        every { validator.validateInstallments(any()) } answers {
            val raw = firstArg<String>()
            val parsed = raw.trim().toIntOrNull() ?: 1
            if (parsed > 12) InstalmentsValidationResult.ExceedsMax(12)
            else InstalmentsValidationResult.Valid(parsed.coerceAtLeast(1))
        }

        viewModel = PaymentViewModel(
            processPayment = processPaymentUseCase,
            cancelTransaction = cancelTransactionUseCase,
            mapper = mapper,
            validator = validator,
            dispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Initial state ──────────────────────────────────────────────────────────

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(PaymentUiState.Idle, viewModel.uiState.first())
    }

    @Test
    fun `initial instalmentSummary is null`() = runTest {
        assertNull(viewModel.instalmentSummary.first())
    }

    @Test
    fun `initial instalmentsValidation is null`() = runTest {
        assertNull(viewModel.instalmentsValidation.first())
    }

    @Test
    fun `initial amountValidation is null`() = runTest {
        assertNull(viewModel.amountValidation.first())
    }

    // ── onInputChanged — real-time instalment error ────────────────────────────

    @Test
    fun `given instalments above limit, when onInputChanged, then instalmentsValidation is ExceedsMax`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")

        assertTrue(viewModel.instalmentsValidation.value is InstalmentsValidationResult.ExceedsMax)
    }

    @Test
    fun `given instalments above limit, when onInputChanged, then instalmentSummary is null`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")

        assertNull(viewModel.instalmentSummary.value)
    }

    @Test
    fun `given valid instalments after invalid, when onInputChanged, then instalmentsValidation clears`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")
        assertNotNull(viewModel.instalmentsValidation.value)

        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "12")
        assertNull(viewModel.instalmentsValidation.value)
    }

    // ── onInputChanged — instalment summary ───────────────────────────────────

    @Test
    fun `given valid amount and 12 instalments, when onInputChanged, then summary is not null`() = runTest {
        viewModel.onInputChanged(rawAmount = 600.0, rawInstalments = "12")

        assertNotNull(viewModel.instalmentSummary.value)
    }

    @Test
    fun `given single instalment and positive amount, when onInputChanged, then summary is not null`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "1")

        assertNotNull(viewModel.instalmentSummary.value)
    }

    @Test
    fun `given zero amount, when onInputChanged, then summary is null`() = runTest {
        viewModel.onInputChanged(rawAmount = 0.0, rawInstalments = "3")

        assertNull(viewModel.instalmentSummary.value)
    }

    // ── processPayment — validation ────────────────────────────────────────────

    @Test
    fun `given invalid amount, when processPayment, then emits ValidationError with amountError`() = runTest {
        every { validator.validateAmount(0.0) } returns AmountValidationResult.AmountZeroOrNegative

        viewModel.processPayment(
            rawAmount = 0.0,
            method = PaymentMethod.CREDIT,
            methodLabel = "Credit",
            statusLabel = "Approved"
        )

        val state = viewModel.uiState.value as PaymentUiState.ValidationError
        assertTrue(state.amountError is AmountValidationResult.AmountZeroOrNegative)
        assertNull(state.instalmentsError)
    }

    @Test
    fun `given instalments above limit on submit, when processPayment, then emits ValidationError with instalmentsError`() = runTest {
        viewModel.processPayment(
            rawAmount = 100.0,
            method = PaymentMethod.CREDIT,
            methodLabel = "Credit",
            statusLabel = "Approved",
            rawInstallments = "600"
        )

        val state = viewModel.uiState.value as PaymentUiState.ValidationError
        assertNull(state.amountError)
        assertTrue(state.instalmentsError is InstalmentsValidationResult.ExceedsMax)
    }

    @Test
    fun `given both fields invalid, when processPayment, then emits ValidationError with both errors`() = runTest {
        every { validator.validateAmount(0.0) } returns AmountValidationResult.AmountZeroOrNegative

        viewModel.processPayment(
            rawAmount = 0.0,
            method = PaymentMethod.CREDIT,
            methodLabel = "Credit",
            statusLabel = "Approved",
            rawInstallments = "600"
        )

        val state = viewModel.uiState.value as PaymentUiState.ValidationError
        assertNotNull(state.amountError)
        assertNotNull(state.instalmentsError)
    }

    // ── processPayment — success ───────────────────────────────────────────────

    @Test
    fun `given valid inputs, when processPayment, then emits Success`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns Result.success(approvedTransaction)
        every { mapper.toUiModel(approvedTransaction, "Credit", "Approved") } returns uiModel

        viewModel.processPayment(
            rawAmount = 100.0,
            method = PaymentMethod.CREDIT,
            methodLabel = "Credit",
            statusLabel = "Approved"
        )

        // With StandardTestDispatcher coroutine is queued but not yet started
        assertEquals(PaymentUiState.Idle, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(PaymentUiState.Success(uiModel), viewModel.uiState.value)
    }

    @Test
    fun `given network failure, when processPayment, then emits Error`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns Result.failure(Exception("Timeout"))

        viewModel.processPayment(
            rawAmount = 100.0,
            method = PaymentMethod.CREDIT,
            methodLabel = "Credit",
            statusLabel = "Approved"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertEquals("Timeout", state.error.message)
        assertFalse(state.isBusinessError)
    }

    @Test
    fun `given business error, when processPayment, then emits Error with isBusinessError true`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns
                Result.failure(InstalmentNotAllowedException(10.0))

        viewModel.processPayment(
            rawAmount = 5.0,
            method = PaymentMethod.CREDIT,
            methodLabel = "Credit",
            statusLabel = "Approved",
            rawInstallments = "2"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertTrue(state.isBusinessError)
        assertTrue(state.error is InstalmentNotAllowedException)
    }

    @Test
    fun `given success, when processPayment, then instalmentsValidation and summary are cleared`() = runTest {
        viewModel.onInputChanged(600.0, "12")
        coEvery { processPaymentUseCase(any()) } returns Result.success(approvedTransaction)
        every { mapper.toUiModel(any(), any(), any()) } returns uiModel

        viewModel.processPayment(
            rawAmount = 600.0,
            method = PaymentMethod.CREDIT,
            methodLabel = "Credit",
            statusLabel = "Approved",
            rawInstallments = "12"
        )
        advanceUntilIdle()

        assertNull(viewModel.instalmentsValidation.value)
        assertNull(viewModel.instalmentSummary.value)
    }

    // ── cancelPreviousTransaction ──────────────────────────────────────────────

    @Test
    fun `given successful cancel, when cancelPreviousTransaction, then emits Success`() = runTest {
        val cancelledTx = approvedTransaction.copy(status = TransactionStatus.CANCELLED)
        coEvery { cancelTransactionUseCase("txn_001") } returns Result.success(cancelledTx)
        every { mapper.toUiModel(cancelledTx, "Credit", "Cancelled") } returns
                uiModel.copy(statusLabel = "Cancelled")

        viewModel.cancelPreviousTransaction("txn_001", "Credit", "Cancelled")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PaymentUiState.Success)
    }

    @Test
    fun `given cancel failure, when cancelPreviousTransaction, then emits Error`() = runTest {
        coEvery { cancelTransactionUseCase(any()) } returns
                Result.failure(Exception("Cannot cancel"))

        viewModel.cancelPreviousTransaction("txn_001", "Credit", "Cancelled")
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertEquals("Cannot cancel", state.error.message)
    }

    // ── isConfirmEnabled ──────────────────────────────────────────────────────

    @Test
    fun `initial isConfirmEnabled is false`() = runTest {
        assertFalse(viewModel.isConfirmEnabled.value)
    }

    @Test
    fun `given no method selected and valid amount, when onInputChanged, then isConfirmEnabled is false`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "")
        assertFalse(viewModel.isConfirmEnabled.value)
    }

    @Test
    fun `given method selected and valid amount, when onMethodSelected, then isConfirmEnabled is true`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.DEBIT, rawAmount = 100.0, rawInstalments = "")
        assertTrue(viewModel.isConfirmEnabled.value)
    }

    @Test
    fun `given method selected and zero amount, when onMethodSelected, then isConfirmEnabled is false`() = runTest {
        every { validator.validateAmount(0.0) } returns AmountValidationResult.AmountZeroOrNegative
        viewModel.onMethodSelected(PaymentMethod.PIX, rawAmount = 0.0, rawInstalments = "")
        assertFalse(viewModel.isConfirmEnabled.value)
    }

    @Test
    fun `given instalments above limit, when onInputChanged, then isConfirmEnabled is false`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.CREDIT, rawAmount = 100.0, rawInstalments = "13")
        assertFalse(viewModel.isConfirmEnabled.value)
    }

    @Test
    fun `given instalments error is corrected, then isConfirmEnabled becomes true`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.CREDIT, rawAmount = 100.0, rawInstalments = "13")
        assertFalse(viewModel.isConfirmEnabled.value)

        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "3")
        assertTrue(viewModel.isConfirmEnabled.value)
    }

    @Test
    fun `when resetState called, then isConfirmEnabled is false`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.PIX, rawAmount = 100.0, rawInstalments = "")
        assertTrue(viewModel.isConfirmEnabled.value)

        viewModel.resetState()
        assertFalse(viewModel.isConfirmEnabled.value)
    }

    // ── onMethodSelected ──────────────────────────────────────────────────────

    @Test
    fun `given method selected, when onMethodSelected, then selectedMethod emits that method`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.PIX, rawAmount = 40.0, rawInstalments = "")
        assertEquals(PaymentMethod.PIX, viewModel.selectedMethod.value)
    }

    @Test
    fun `given credit selected with amount above minimum, when onMethodSelected, then instalmentInputVisible is true`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.CREDIT, rawAmount = 40.0, rawInstalments = "")
        assertTrue(viewModel.instalmentInputVisible.value)
    }

    @Test
    fun `given credit selected with amount below minimum, when onMethodSelected, then instalmentInputVisible is false`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.CREDIT, rawAmount = 5.0, rawInstalments = "")
        assertFalse(viewModel.instalmentInputVisible.value)
    }

    @Test
    fun `when resetState called, then selectedMethod is null`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.PIX, rawAmount = 40.0, rawInstalments = "")
        viewModel.resetState()
        assertNull(viewModel.selectedMethod.value)
    }

    // ── instalmentInputVisible ─────────────────────────────────────────────────

    @Test
    fun `given amount below minimum, when onInputChanged, then instalmentInputVisible is false`() = runTest {
        viewModel.onInputChanged(rawAmount = 9.99, rawInstalments = "")
        assertFalse(viewModel.instalmentInputVisible.value)
    }

    @Test
    fun `given amount exactly at minimum, when onInputChanged, then instalmentInputVisible is true`() = runTest {
        viewModel.onInputChanged(rawAmount = 10.0, rawInstalments = "")
        assertTrue(viewModel.instalmentInputVisible.value)
    }

    @Test
    fun `given amount drops below minimum, when onInputChanged, then instalmentInputVisible becomes false`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "3")
        assertTrue(viewModel.instalmentInputVisible.value)

        viewModel.onInputChanged(rawAmount = 5.0, rawInstalments = "")
        assertFalse(viewModel.instalmentInputVisible.value)
    }

    @Test
    fun `given amount below minimum, when onInputChanged, then instalmentsValidation is cleared`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")
        assertNotNull(viewModel.instalmentsValidation.value)

        viewModel.onInputChanged(rawAmount = 5.0, rawInstalments = "13")
        assertNull(viewModel.instalmentsValidation.value)
    }

    @Test
    fun `when resetState called, then instalmentInputVisible is false`() = runTest {
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "")
        assertTrue(viewModel.instalmentInputVisible.value)

        viewModel.resetState()
        assertFalse(viewModel.instalmentInputVisible.value)
    }

    // ── resetState ─────────────────────────────────────────────────────────────

    @Test
    fun `when resetState called, then all flows are cleared`() = runTest {
        viewModel.onMethodSelected(PaymentMethod.CREDIT, rawAmount = 100.0, rawInstalments = "12")

        viewModel.resetState()

        assertEquals(PaymentUiState.Idle, viewModel.uiState.value)
        assertNull(viewModel.selectedMethod.value)
        assertNull(viewModel.instalmentSummary.value)
        assertNull(viewModel.instalmentsValidation.value)
        assertNull(viewModel.amountValidation.value)
        assertFalse(viewModel.instalmentInputVisible.value)
        assertFalse(viewModel.isConfirmEnabled.value)
    }
}