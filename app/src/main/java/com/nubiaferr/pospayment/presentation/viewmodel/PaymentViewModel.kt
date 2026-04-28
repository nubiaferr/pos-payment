package com.nubiaferr.pospayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nubiaferr.pospayment.domain.exception.BusinessException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.strategy.CreditPaymentStrategy
import com.nubiaferr.pospayment.domain.usecase.CancelTransactionUseCase
import com.nubiaferr.pospayment.domain.usecase.ProcessPaymentUseCase
import com.nubiaferr.pospayment.domain.validation.AmountValidationResult
import com.nubiaferr.pospayment.domain.validation.InstalmentsValidationResult
import com.nubiaferr.pospayment.domain.validation.PaymentInputValidator
import com.nubiaferr.pospayment.presentation.mapper.PaymentUiMapper
import com.nubiaferr.pospayment.presentation.uistate.PaymentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the payment screen.
 *
 * @property processPayment    Initiates a payment through the domain.
 * @property cancelTransaction Reverses an approved transaction.
 * @property mapper            Converts domain entities to pre-formatted UI models.
 * @property validator         Validates generic input boundaries.
 * @property dispatcher        Coroutine dispatcher — injectable for testing.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val processPayment: ProcessPaymentUseCase,
    private val cancelTransaction: CancelTransactionUseCase,
    private val mapper: PaymentUiMapper,
    private val validator: PaymentInputValidator,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    /**
     * Per-instalment breakdown for real-time display — formatted by [mapper].
     * e.g. "12x R$ 50,00". Null when not applicable.
     */
    private val _instalmentSummary = MutableStateFlow<String?>(null)
    val instalmentSummary: StateFlow<String?> = _instalmentSummary.asStateFlow()

    /**
     * Typed instalment validation result emitted on every keystroke.
     * [com.nubiaferr.pospayment.presentation.fragment.PaymentFragment] resolves this to a string via [InstalmentsValidationResult].
     * Null when valid or field is not visible.
     */
    private val _instalmentsValidation = MutableStateFlow<InstalmentsValidationResult?>(null)
    val instalmentsValidation: StateFlow<InstalmentsValidationResult?> = _instalmentsValidation.asStateFlow()

    /**
     * Typed amount validation result emitted on every keystroke.
     * [com.nubiaferr.pospayment.presentation.fragment.PaymentFragment] resolves this to a string via [AmountValidationResult].
     * Null when valid.
     */
    private val _amountValidation = MutableStateFlow<AmountValidationResult?>(null)
    val amountValidation: StateFlow<AmountValidationResult?> = _amountValidation.asStateFlow()

    /** The currently selected payment method. */
    private val _selectedMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedMethod: StateFlow<PaymentMethod?> = _selectedMethod.asStateFlow()

    /** Whether the confirm button should be enabled. */
    private val _isConfirmEnabled = MutableStateFlow(false)
    val isConfirmEnabled: StateFlow<Boolean> = _isConfirmEnabled.asStateFlow()

    /** Controls instalment input row visibility when Credit is selected. */
    private val _instalmentInputVisible = MutableStateFlow(false)
    val instalmentInputVisible: StateFlow<Boolean> = _instalmentInputVisible.asStateFlow()

    fun onMethodSelected(method: PaymentMethod, rawAmount: Double, rawInstalments: String) {
        _selectedMethod.value = method
        onInputChanged(rawAmount, rawInstalments)
    }

    /**
     * Called on every keystroke. Updates all reactive fields in real time.
     */
    fun onInputChanged(rawAmount: Double, rawInstalments: String) {
        val meetsMinimum = rawAmount >= CreditPaymentStrategy.MIN_INSTALMENT_AMOUNT
        _instalmentInputVisible.value = meetsMinimum

        if (!meetsMinimum) {
            _amountValidation.value = null
            _instalmentsValidation.value = null
            _instalmentSummary.value = null
            _isConfirmEnabled.value = evaluateConfirmEnabled(rawAmount, hasInstalmentsError = false)
            return
        }

        val amountResult = validator.validateAmount(rawAmount)
        _amountValidation.value = if (amountResult is AmountValidationResult.Valid) null else amountResult

        if (amountResult !is AmountValidationResult.Valid) {
            _instalmentsValidation.value = null
            _instalmentSummary.value = null
            _isConfirmEnabled.value = false
            return
        }

        val instalmentsResult = validator.validateInstallments(rawInstalments)
        val hasInstalmentsError = instalmentsResult is InstalmentsValidationResult.ExceedsMax
        _instalmentsValidation.value = if (hasInstalmentsError) instalmentsResult else null
        _instalmentSummary.value = if (!hasInstalmentsError) {
            mapper.formatInstalmentSummary(
                instalments = (instalmentsResult as InstalmentsValidationResult.Valid).installments,
                amountPerInstalment = rawAmount / instalmentsResult.installments
            )
        } else null

        _isConfirmEnabled.value = evaluateConfirmEnabled(rawAmount, hasInstalmentsError)
    }

    /**
     * Validates generic input and, if valid, submits the payment to the use case.
     *
     * Per-method limit violations surface as [PaymentUiState.Error] when the
     * strategy rejects the payment after submission.
     *
     * @param methodLabel  Pre-resolved label string for the receipt (e.g. "Credit").
     * @param statusLabel  Pre-resolved label string for the receipt (e.g. "Approved").
     *                     Passed from Fragment so the mapper stays free of Context.
     */
    fun processPayment(
        rawAmount: Double,
        method: PaymentMethod,
        methodLabel: String,
        statusLabel: String,
        rawInstallments: String = "",
        description: String = ""
    ) {
        val amountResult = validator.validateAmount(rawAmount)
        val instalmentsResult = validator.validateInstallments(rawInstallments)

        val hasAmountError = amountResult !is AmountValidationResult.Valid
        val hasInstalmentsError = instalmentsResult is InstalmentsValidationResult.ExceedsMax

        if (hasAmountError || hasInstalmentsError) {
            _uiState.value = PaymentUiState.ValidationError(
                amountError = if (hasAmountError) amountResult else null,
                instalmentsError = if (hasInstalmentsError) instalmentsResult else null
            )
            return
        }

        val amount = (amountResult as AmountValidationResult.Valid).amount
        val installments = (instalmentsResult as InstalmentsValidationResult.Valid).installments

        viewModelScope.launch(dispatcher) {
            _uiState.value = PaymentUiState.Loading
            processPayment(
                Payment(
                    amount = amount,
                    method = method,
                    installments = installments,
                    description = description
                )
            ).fold(
                onSuccess = { transaction ->
                    _instalmentSummary.value = null
                    _instalmentsValidation.value = null
                    _uiState.value = PaymentUiState.Success(
                        mapper.toUiModel(transaction, methodLabel, statusLabel)
                    )
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error = error,
                        isBusinessError = error is BusinessException
                    )
                }
            )
        }
    }

    fun cancelPreviousTransaction(transactionId: String, methodLabel: String, statusLabel: String) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = PaymentUiState.Loading
            cancelTransaction(transactionId).fold(
                onSuccess = { transaction ->
                    _uiState.value = PaymentUiState.Success(
                        mapper.toUiModel(transaction, methodLabel, statusLabel)
                    )
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        error = error,
                        isBusinessError = error is BusinessException
                    )
                }
            )
        }
    }

    fun resetState() {
        _selectedMethod.value = null
        _instalmentSummary.value = null
        _instalmentsValidation.value = null
        _amountValidation.value = null
        _instalmentInputVisible.value = false
        _isConfirmEnabled.value = false
        _uiState.value = PaymentUiState.Idle
    }

    private fun evaluateConfirmEnabled(rawAmount: Double, hasInstalmentsError: Boolean): Boolean {
        if (_selectedMethod.value == null) return false
        if (hasInstalmentsError) return false
        return validator.validateAmount(rawAmount) is AmountValidationResult.Valid
    }
}