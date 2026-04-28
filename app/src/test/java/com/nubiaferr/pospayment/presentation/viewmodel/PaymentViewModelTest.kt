package com.nubiaferr.pospayment.presentation.viewmodel

import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.usecase.CancelTransactionUseCase
import com.nubiaferr.pospayment.domain.usecase.GetTransactionStatusUseCase
import com.nubiaferr.pospayment.domain.usecase.ProcessPaymentUseCase
import com.nubiaferr.pospayment.presentation.mapper.PaymentUiMapper
import com.nubiaferr.pospayment.presentation.model.TransactionUiModel
import com.nubiaferr.pospayment.presentation.uistate.PaymentUiState
import io.mockk.coEvery
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
 * Uses [StandardTestDispatcher] injected via the dispatcher parameter
 * so coroutines are fully controlled in tests without real delays.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var processPaymentUseCase: ProcessPaymentUseCase
    private lateinit var cancelTransactionUseCase: CancelTransactionUseCase
    private lateinit var getTransactionStatusUseCase: GetTransactionStatusUseCase
    private lateinit var mapper: PaymentUiMapper
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

        viewModel = PaymentViewModel(
            processPayment = processPaymentUseCase,
            cancelTransaction = cancelTransactionUseCase,
            getTransactionStatus = getTransactionStatusUseCase,
            mapper = mapper,
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

    // ── processPayment ─────────────────────────────────────────────────────────

    @Test
    fun `given successful payment, when processPayment called, then emits Idle then Success`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns Result.success(approvedTransaction)
        coEvery { mapper.toUiModel(approvedTransaction) } returns uiModel

        viewModel.processPayment(amount = 100.0, method = PaymentMethod.CREDIT)

        // With StandardTestDispatcher, the coroutine is queued but not yet started —
        // state remains Idle until advanceUntilIdle() runs it.
        assertEquals(PaymentUiState.Idle, viewModel.uiState.value)

        advanceUntilIdle()

        assertEquals(PaymentUiState.Success(uiModel), viewModel.uiState.value)
    }

    @Test
    fun `given network failure, when processPayment called, then emits Error`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns Result.failure(Exception("Network timeout"))

        viewModel.processPayment(amount = 100.0, method = PaymentMethod.CREDIT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is PaymentUiState.Error)
        assertEquals("Network timeout", (state as PaymentUiState.Error).message)
    }

    @Test
    fun `given business rule violation, when processPayment called, then emits Error with isBusinessError true`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns
                Result.failure(InstalmentNotAllowedException(10.0))

        viewModel.processPayment(amount = 5.0, method = PaymentMethod.CREDIT, installments = 2)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertTrue(state.isBusinessError)
    }

    @Test
    fun `given failure with null message, when processPayment called, then emits generic error`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns
                Result.failure(Exception(null as String?))

        viewModel.processPayment(amount = 100.0, method = PaymentMethod.CREDIT)
        advanceUntilIdle()

        val state = viewModel.uiState.value as PaymentUiState.Error
        assertEquals("Erro desconhecido", state.message)
    }

    // ── cancelPreviousTransaction ──────────────────────────────────────────────

    @Test
    fun `given successful cancel, when cancelPreviousTransaction called, then emits Success`() = runTest {
        val cancelledTx = approvedTransaction.copy(status = TransactionStatus.CANCELLED)
        coEvery { cancelTransactionUseCase("txn_vm_001") } returns Result.success(cancelledTx)
        coEvery { mapper.toUiModel(cancelledTx) } returns uiModel.copy(statusLabel = "Cancelado")

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
        coEvery { mapper.toUiModel(approvedTransaction) } returns uiModel

        viewModel.checkTransactionStatus("txn_vm_001")
        advanceUntilIdle()

        assertEquals(PaymentUiState.Success(uiModel), viewModel.uiState.value)
    }

    // ── resetState ─────────────────────────────────────────────────────────────

    @Test
    fun `when resetState called, then state returns to Idle`() = runTest {
        coEvery { processPaymentUseCase(any()) } returns Result.failure(Exception("err"))

        viewModel.processPayment(amount = 100.0, method = PaymentMethod.CREDIT)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is PaymentUiState.Error)

        viewModel.resetState()

        assertEquals(PaymentUiState.Idle, viewModel.uiState.value)
    }
}