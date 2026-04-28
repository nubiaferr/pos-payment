package com.nubiaferr.pospayment.presentation.viewmodel

import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.usecase.CancelTransactionUseCase
import com.nubiaferr.pospayment.domain.usecase.GetTransactionStatusUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
        id = "txn_001",
        payment = Payment(100.0, PaymentMethod.CREDIT),
        status = TransactionStatus.APPROVED,
        authCode = "AUTH",
        timestamp = 1_700_000_000_000L
    )

    private val uiModel = TransactionUiModel(
        id = "txn_001",
        formattedAmount = "R$ 100,00",
        methodLabel = "Crédito",
        instalments = "",
        authCode = "AUTH",
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

    @Test
    fun `initial instalmentSummary is null`() = runTest {
        assertNull(viewModel.instalmentSummary.first())
    }

    @Test
    fun `initial instalmentsError is null`() = runTest {
        assertNull(viewModel.instalmentsError.first())
    }

    // ── onInputChanged — real-time instalment error ────────────────────────────

    @Test
    fun `given instalments above limit, when onInputChanged, then instalmentsError is set immediately`() =
        runTest {
            every { validator.validateInstallments("13") } returns
                    InstalmentsValidationResult.Invalid("Máximo de 12 parcelas permitidas")

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")

            assertNotNull(viewModel.instalmentsError.value)
            assertTrue(viewModel.instalmentsError.value!!.contains("12"))
        }

    @Test
    fun `given instalments above limit, when onInputChanged, then instalmentSummary is null`() =
        runTest {
            every { validator.validateInstallments("13") } returns
                    InstalmentsValidationResult.Invalid("Máximo de 12 parcelas permitidas")

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")

            assertNull(viewModel.instalmentSummary.value)
        }

    @Test
    fun `given valid instalments after invalid, when onInputChanged, then instalmentsError clears`() =
        runTest {
            every { validator.validateInstallments("13") } returns
                    InstalmentsValidationResult.Invalid("Máximo de 12 parcelas permitidas")
            every { validator.validateInstallments("12") } returns
                    InstalmentsValidationResult.Valid(12)

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")
            assertNotNull(viewModel.instalmentsError.value)

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "12")
            assertNull(viewModel.instalmentsError.value)
        }

    // ── onInputChanged — instalment summary ───────────────────────────────────

    @Test
    fun `given valid amount and instalments greater than 1, when onInputChanged, then summary is not null`() =
        runTest {
            every { validator.validateInstallments("12") } returns InstalmentsValidationResult.Valid(
                12
            )

            viewModel.onInputChanged(rawAmount = 600.0, rawInstalments = "12")

            assertNotNull(viewModel.instalmentSummary.value)
            assertTrue(viewModel.instalmentSummary.value!!.contains("12x"))
        }

    @Test
    fun `given single instalment, when onInputChanged, then summary is null`() = runTest {
        every { validator.validateInstallments("1") } returns InstalmentsValidationResult.Valid(1)

        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "1")

        assertNull(viewModel.instalmentSummary.value)
    }

    @Test
    fun `given zero amount with valid instalments, when onInputChanged, then summary is null`() =
        runTest {
            every { validator.validateInstallments("3") } returns InstalmentsValidationResult.Valid(
                3
            )

            viewModel.onInputChanged(rawAmount = 0.0, rawInstalments = "3")

            assertNull(viewModel.instalmentSummary.value)
        }

    // ── processPayment — validation ────────────────────────────────────────────

    @Test
    fun `given invalid amount, when processPayment, then emits ValidationError with amountError`() =
        runTest {
            every { validator.validateAmount(0.0) } returns AmountValidationResult.Invalid("O valor deve ser maior que zero")
            every { validator.validateInstallments(any()) } returns InstalmentsValidationResult.Valid(
                1
            )

            viewModel.processPayment(rawAmount = 0.0, method = PaymentMethod.CREDIT)

            val state = viewModel.uiState.value as PaymentUiState.ValidationError
            assertNotNull(state.amountError)
            assertNull(state.instalmentsError)
        }

    @Test
    fun `given instalments above limit on submit, when processPayment, then emits ValidationError with instalmentsError`() =
        runTest {
            every { validator.validateAmount(100.0) } returns AmountValidationResult.Valid(100.0)
            every { validator.validateInstallments("600") } returns
                    InstalmentsValidationResult.Invalid("Máximo de 12 parcelas permitidas")

            viewModel.processPayment(
                rawAmount = 100.0,
                method = PaymentMethod.CREDIT,
                rawInstallments = "600"
            )

            val state = viewModel.uiState.value as PaymentUiState.ValidationError
            assertNull(state.amountError)
            assertNotNull(state.instalmentsError)
        }

    @Test
    fun `given both fields invalid, when processPayment, then emits ValidationError with both errors`() =
        runTest {
            every { validator.validateAmount(0.0) } returns AmountValidationResult.Invalid("O valor deve ser maior que zero")
            every { validator.validateInstallments("600") } returns
                    InstalmentsValidationResult.Invalid("Máximo de 12 parcelas permitidas")

            viewModel.processPayment(
                rawAmount = 0.0,
                method = PaymentMethod.CREDIT,
                rawInstallments = "600"
            )

            val state = viewModel.uiState.value as PaymentUiState.ValidationError
            assertNotNull(state.amountError)
            assertNotNull(state.instalmentsError)
        }

    // ── processPayment — success ───────────────────────────────────────────────

    @Test
    fun `given valid inputs, when processPayment, then emits Success`() = runTest {
        every { validator.validateAmount(100.0) } returns AmountValidationResult.Valid(100.0)
        every { validator.validateInstallments("") } returns InstalmentsValidationResult.Valid(1)
        coEvery { processPaymentUseCase(any()) } returns Result.success(approvedTransaction)
        every { mapper.toUiModel(approvedTransaction) } returns uiModel

        viewModel.processPayment(rawAmount = 100.0, method = PaymentMethod.CREDIT)
        advanceUntilIdle()

        assertEquals(PaymentUiState.Success(uiModel), viewModel.uiState.value)
    }

    @Test
    fun `given success, when processPayment, then instalmentsError and summary are cleared`() =
        runTest {
            every { validator.validateInstallments("12") } returns InstalmentsValidationResult.Valid(
                12
            )
            viewModel.onInputChanged(600.0, "12")

            every { validator.validateAmount(600.0) } returns AmountValidationResult.Valid(600.0)
            every { validator.validateInstallments(any()) } returns InstalmentsValidationResult.Valid(
                12
            )
            coEvery { processPaymentUseCase(any()) } returns Result.success(approvedTransaction)
            every { mapper.toUiModel(any()) } returns uiModel

            viewModel.processPayment(
                rawAmount = 600.0,
                method = PaymentMethod.CREDIT,
                rawInstallments = "12"
            )
            advanceUntilIdle()

            assertNull(viewModel.instalmentsError.value)
            assertNull(viewModel.instalmentSummary.value)
        }

    // ── resetState ─────────────────────────────────────────────────────────────

    @Test
    fun `when resetState called, then state is Idle and all auxiliary flows are cleared`() =
        runTest {
            every { validator.validateInstallments("13") } returns
                    InstalmentsValidationResult.Invalid("Máximo de 12 parcelas permitidas")

            viewModel.onInputChanged(100.0, "13")
            viewModel.resetState()

            assertEquals(PaymentUiState.Idle, viewModel.uiState.value)
            assertNull(viewModel.instalmentsError.value)
            assertNull(viewModel.instalmentSummary.value)
        }

// ── onInputChanged — instalmentInputVisible ────────────────────────────────

    @Test
    fun `given amount below minimum, when onInputChanged, then instalmentInputVisible is false`() =
        runTest {
            viewModel.onInputChanged(rawAmount = 9.99, rawInstalments = "")

            assertFalse(viewModel.instalmentInputVisible.value)
        }

    @Test
    fun `given amount exactly at minimum, when onInputChanged, then instalmentInputVisible is true`() =
        runTest {
            every { validator.validateInstallments(any()) } returns InstalmentsValidationResult.Valid(
                1
            )

            viewModel.onInputChanged(rawAmount = 10.0, rawInstalments = "")

            assertTrue(viewModel.instalmentInputVisible.value)
        }

    @Test
    fun `given amount above minimum, when onInputChanged, then instalmentInputVisible is true`() =
        runTest {
            every { validator.validateInstallments(any()) } returns InstalmentsValidationResult.Valid(
                1
            )

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "")

            assertTrue(viewModel.instalmentInputVisible.value)
        }

    @Test
    fun `given amount drops below minimum after being above, when onInputChanged, then instalmentInputVisible becomes false`() =
        runTest {
            every { validator.validateInstallments(any()) } returns InstalmentsValidationResult.Valid(
                3
            )

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "3")
            assertTrue(viewModel.instalmentInputVisible.value)

            viewModel.onInputChanged(rawAmount = 5.0, rawInstalments = "")
            assertFalse(viewModel.instalmentInputVisible.value)
        }

    @Test
    fun `given amount below minimum, when onInputChanged, then instalmentsError is cleared`() =
        runTest {
            every { validator.validateInstallments("13") } returns
                    InstalmentsValidationResult.Invalid("Máximo de 12 parcelas permitidas")

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "13")
            assertNotNull(viewModel.instalmentsError.value)

            viewModel.onInputChanged(rawAmount = 5.0, rawInstalments = "13")
            assertNull(viewModel.instalmentsError.value)
        }

    @Test
    fun `given amount below minimum, when onInputChanged, then instalmentSummary is cleared`() =
        runTest {
            every { validator.validateInstallments("3") } returns InstalmentsValidationResult.Valid(
                3
            )

            viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "3")
            assertNotNull(viewModel.instalmentSummary.value)

            viewModel.onInputChanged(rawAmount = 5.0, rawInstalments = "3")
            assertNull(viewModel.instalmentSummary.value)
        }

    @Test
    fun `when resetState called, then instalmentInputVisible is false`() = runTest {
        every { validator.validateInstallments(any()) } returns InstalmentsValidationResult.Valid(1)
        viewModel.onInputChanged(rawAmount = 100.0, rawInstalments = "")
        assertTrue(viewModel.instalmentInputVisible.value)

        viewModel.resetState()

        assertFalse(viewModel.instalmentInputVisible.value)
    }
}