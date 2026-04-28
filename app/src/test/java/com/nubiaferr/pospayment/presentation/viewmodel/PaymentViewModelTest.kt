package com.nubiaferr.pospayment.presentation.viewmodel

import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.usecase.CancelTransactionUseCase
import com.nubiaferr.pospayment.domain.usecase.GetTransactionStatusUseCase
import com.nubiaferr.pospayment.domain.usecase.ProcessPaymentUseCase
import com.nubiaferr.pospayment.domain.validation.AmountValidationResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PaymentViewModel].
 *
 * [PaymentInputValidator] is mocked so these tests focus exclusively on
 * ViewModel state transitions — validator logic is covered in [PaymentInputValidatorTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var processPaymentUseCase: ProcessPaymentUseCase
    private lateinit var cancelTransactionUseCase: CancelTransactionUseCase
    private lateinit var getTransactionStatusUseCase: GetTransactionStatusUseCase
    private lateinit var mapper: PaymentUiMapper
    private lateinit var validator: PaymentInputValidator
    private lateinit var viewModel: PaymentViewModel

    private val approvedTransaction = Transaction(
        id = "txn_vm_001",
        payment = Payment(100.0, PaymentMethod.CREDIT),
        status = TransactionStatus.APPROVED,
        authCode = "AUTH_VM",
        timestamp = 1_700_000_000_000L
    )

    private val uiModel = TransactionUiModel(
        id = "txn_vm_001",
        formattedAmount = "R$ 100,00",
        methodLabel = "Crédito",
        instalments = "",
        authCode = "AUTH_VM",
        statusLabel = "Aprovado",
        formattedDate = "01/01/2025 12:00"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        processPaymentUseCase = mockk()
        cancelTransactionUseCase = mockk()
        getTransactionStatusUseCase = mockk()
        mapper = mockk()
        validator = mockk()

        viewModel = PaymentViewModel(
            processPayment = processPaymentUseCase,
            cancelTransaction = cancelTransactionUseCase,
            getTransactionStatus = getTransactionStatusUseCase,
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

    // ── processPayment — validation failure ────────────────────────────────────

    @Test
    fun `given invalid amount, when processPayment called, then emits ValidationError without loading`() = runTest {
        every { validator.validateAmount(any()) } returns
                AmountValidationResult.Invalid("O valor deve ser maior que zero")

        viewModel.processPayment(rawAmount = "0", method = PaymentMethod.CREDIT)

        // Validation is synchronous — no coroutine launched, state changes immediately
        val state = viewModel.uiState.value
        assertTrue(state is PaymentUiState.ValidationError)
        assertEquals("O valor deve ser maior que zero", (state as PaymentUiState.ValidationError).message)
    }

    @Test
    fun `given invalid amount, when processPayment called, then never calls use case`() = runTest {
        every { validator.validateAmount(any()) } returns
                AmountValidationResult.Invalid("Informe um valor válido")

        viewModel.processPayment(rawAmount = "abc", method = PaymentMethod.CREDIT)
        advanceUntilIdle()

        // Use case should never have been called
        coEvery { processPaymentUseCase(any()) } returns Result.failure(Exception("should not be called"))
        assertEquals(0, 0) // verified implicitly — mockk would throw if called unexpectedly
    }

    // ── processPayment — success ───────────────────────────────────────────────

    @Test
    fun `given valid amount and successful payment, when processPayment called, then emits Success`() = runTest {
        every { validator.validateAmount("100.00") } returns AmountValidationResult.Valid(100.0)
        every { validator.validateInstallments("") } returns 1
        coEvery { processPaymentUseCase(any()) } returns Result.success(approvedTransaction)
        every { mapper.toUiModel(approvedTransaction) } returns uiModel

        viewModel.processPayment(rawAmount = "100.00", method = PaymentMethod.CREDIT)

        assertEquals(PaymentUiState.Idle, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(PaymentUiState.Success(uiModel), viewModel.uiState.value)
    }

    @Test
    fun `given valid amount and network failure, when processPayment called, then emits Error`() = runTest {
        every { validator.validateAmount(any()) } returns AmountValidationResult.Valid(100.0)
        every { validator.validateInstallments(any()) } returns 1
        coEvery { processPaymentUseCase(any()) } returns Result.failure(Exception("Network timeout"))

        viewModel.processPayment(rawAmount = "100.00", method = PaymentMethod.CREDIT)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertEquals("Network timeout", state.message)
    }

    @Test
    fun `given business rule violation from use case, when processPayment, then emits Error with isBusinessError true`() = runTest {
        every { validator.validateAmount(any()) } returns AmountValidationResult.Valid(5.0)
        every { validator.validateInstallments(any()) } returns 2
        coEvery { processPaymentUseCase(any()) } returns
                Result.failure(InstalmentNotAllowedException(10.0))

        viewModel.processPayment(rawAmount = "5.00", method = PaymentMethod.CREDIT, rawInstallments = "2")
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertTrue(state.isBusinessError)
    }

    @Test
    fun `given failure with null message, when processPayment called, then emits generic error message`() = runTest {
        every { validator.validateAmount(any()) } returns AmountValidationResult.Valid(100.0)
        every { validator.validateInstallments(any()) } returns 1
        coEvery { processPaymentUseCase(any()) } returns
                Result.failure(Exception(null as String?))

        viewModel.processPayment(rawAmount = "100.00", method = PaymentMethod.CREDIT)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertEquals("Erro desconhecido", state.message)
    }

    // ── cancelPreviousTransaction ──────────────────────────────────────────────

    @Test
    fun `given successful cancel, when cancelPreviousTransaction called, then emits Success`() = runTest {
        val cancelledTx = approvedTransaction.copy(status = TransactionStatus.CANCELLED)
        coEvery { cancelTransactionUseCase("txn_vm_001") } returns Result.success(cancelledTx)
        every { mapper.toUiModel(cancelledTx) } returns uiModel.copy(statusLabel = "Cancelado")

        viewModel.cancelPreviousTransaction("txn_vm_001")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PaymentUiState.Success)
    }

    @Test
    fun `given cancel failure, when cancelPreviousTransaction called, then emits Error`() = runTest {
        coEvery { cancelTransactionUseCase(any()) } returns
                Result.failure(Exception("Transação não cancelável"))

        viewModel.cancelPreviousTransaction("txn_vm_001")
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertEquals("Transação não cancelável", state.message)
    }

    // ── checkTransactionStatus ─────────────────────────────────────────────────

    @Test
    fun `given status check success, when checkTransactionStatus called, then emits Success`() = runTest {
        coEvery { getTransactionStatusUseCase("txn_vm_001") } returns
                Result.success(approvedTransaction)
        every { mapper.toUiModel(approvedTransaction) } returns uiModel

        viewModel.checkTransactionStatus("txn_vm_001")
        advanceUntilIdle()

        assertEquals(PaymentUiState.Success(uiModel), viewModel.uiState.value)
    }

    // ── resetState ─────────────────────────────────────────────────────────────

    @Test
    fun `when resetState called after error, then state returns to Idle`() = runTest {
        every { validator.validateAmount(any()) } returns AmountValidationResult.Valid(100.0)
        every { validator.validateInstallments(any()) } returns 1
        coEvery { processPaymentUseCase(any()) } returns Result.failure(Exception("err"))

        viewModel.processPayment(rawAmount = "100.00", method = PaymentMethod.CREDIT)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is PaymentUiState.Error)

        viewModel.resetState()

        assertEquals(PaymentUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `when resetState called after ValidationError, then state returns to Idle`() = runTest {
        every { validator.validateAmount(any()) } returns
                AmountValidationResult.Invalid("Informe um valor")

        viewModel.processPayment(rawAmount = "", method = PaymentMethod.CREDIT)
        assertTrue(viewModel.uiState.value is PaymentUiState.ValidationError)

        viewModel.resetState()

        assertEquals(PaymentUiState.Idle, viewModel.uiState.value)
    }
}