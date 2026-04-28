package com.nubiaferr.pospayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nubiaferr.pospayment.domain.exception.BusinessException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.strategy.CreditPaymentStrategy
import com.nubiaferr.pospayment.domain.usecase.CancelTransactionUseCase
import com.nubiaferr.pospayment.domain.usecase.GetTransactionStatusUseCase
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
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val processPayment: ProcessPaymentUseCase,
    private val cancelTransaction: CancelTransactionUseCase,
    private val getTransactionStatus: GetTransactionStatusUseCase,
    private val mapper: PaymentUiMapper,
    private val validator: PaymentInputValidator,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    /**
     * Per-instalment value shown in real time below the instalments field.
     * e.g. "12x de R$ 50,00". Null when not applicable.
     */
    private val _instalmentSummary = MutableStateFlow<String?>(null)
    val instalmentSummary: StateFlow<String?> = _instalmentSummary.asStateFlow()

    /**
     * Inline error for the instalments field, updated on every keystroke.
     * Null when the value is valid or the field is blank.
     */
    private val _instalmentsError = MutableStateFlow<String?>(null)
    val instalmentsError: StateFlow<String?> = _instalmentsError.asStateFlow()

    /**
     * Controls the instalment input field visibility when Credit is selected.
     *
     * - `true`  → amount >= R$ 10,00: show input, hide the minimum notice
     * - `false` → amount < R$ 10,00:  hide input, show the minimum notice
     *
     * Only relevant when the selected method is Credit. The Fragment is
     * responsible for ignoring this flow for other methods.
     */
    private val _instalmentInputVisible = MutableStateFlow(false)
    val instalmentInputVisible: StateFlow<Boolean> = _instalmentInputVisible.asStateFlow()

    /**
     * The currently selected payment method.
     * Kept in the ViewModel so the Fragment can observe and highlight the
     * correct button reactively — including on config changes.
     */
    private val _selectedMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedMethod: StateFlow<PaymentMethod?> = _selectedMethod.asStateFlow()

    /**
     * Whether the confirm button should be enabled.
     * True when a method is selected, amount is valid, and there are no instalment errors.
     */
    private val _isConfirmEnabled = MutableStateFlow(false)
    val isConfirmEnabled: StateFlow<Boolean> = _isConfirmEnabled.asStateFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /**
     * Called on every keystroke in either the amount or instalments field.
     *
     * Updates in real time:
     * - [instalmentInputVisible] based on whether amount meets the minimum
     * - [instalmentSummary] with the per-instalment breakdown
     * - [instalmentsError] when the instalment count exceeds the maximum
     */
    /**
     * Called when the operator selects a payment method.
     * Updates [selectedMethod] and triggers an immediate input evaluation
     * so [instalmentInputVisible] reflects the current amount without
     * requiring the operator to change the value.
     */
    fun onMethodSelected(method: PaymentMethod, rawAmount: Double, rawInstalments: String) {
        _selectedMethod.value = method
        onInputChanged(rawAmount, rawInstalments)
    }

    fun onInputChanged(rawAmount: Double, rawInstalments: String) {
        val meetsMinimum = rawAmount >= CreditPaymentStrategy.MIN_INSTALMENT_AMOUNT
        _instalmentInputVisible.value = meetsMinimum

        if (!meetsMinimum) {
            _instalmentSummary.value = null
            _instalmentsError.value = null
            _isConfirmEnabled.value = evaluateConfirmEnabled(
                rawAmount = rawAmount,
                instalmentsError = null
            )
            return
        }

        val instalmentsResult = validator.validateInstallments(rawInstalments)
        when (instalmentsResult) {
            is InstalmentsValidationResult.Invalid -> {
                _instalmentsError.value = instalmentsResult.message
                _instalmentSummary.value = null
            }
            is InstalmentsValidationResult.Valid -> {
                _instalmentsError.value = null
                _instalmentSummary.value = if (rawAmount > 0.0) {
                    val perInstalment = rawAmount / instalmentsResult.installments
                    "${instalmentsResult.installments}x de ${currencyFormatter.format(perInstalment)}"
                } else {
                    null
                }
            }
        }

        _isConfirmEnabled.value = evaluateConfirmEnabled(
            rawAmount = rawAmount,
            instalmentsError = _instalmentsError.value
        )
    }

    /**
     * Returns true when the form is in a submittable state:
     * - A method is selected
     * - Amount is positive and within limits
     * - No instalment errors
     */
    private fun evaluateConfirmEnabled(
        rawAmount: Double,
        instalmentsError: String?
    ): Boolean {
        if (_selectedMethod.value == null) return false
        if (instalmentsError != null) return false
        return validator.validateAmount(rawAmount) is AmountValidationResult.Valid
    }

    /**
     * Validates both fields and, if valid, initiates a payment.
     * Both errors are collected before emitting so the operator sees
     * all problems at once.
     */
    fun processPayment(
        rawAmount: Double,
        method: PaymentMethod,
        rawInstallments: String = "",
        description: String = ""
    ) {
        val amountResult = validator.validateAmount(rawAmount)
        val instalmentsResult = validator.validateInstallments(rawInstallments)

        val amountError = (amountResult as? AmountValidationResult.Invalid)?.message
        val instalmentsError = (instalmentsResult as? InstalmentsValidationResult.Invalid)?.message

        if (amountError != null || instalmentsError != null) {
            _uiState.value = PaymentUiState.ValidationError(
                amountError = amountError,
                instalmentsError = instalmentsError
            )
            return
        }

        val amount = (amountResult as AmountValidationResult.Valid).amount
        val installments = (instalmentsResult as InstalmentsValidationResult.Valid).installments

        viewModelScope.launch(dispatcher) {
            _uiState.value = PaymentUiState.Loading
            val payment = Payment(
                amount = amount,
                method = method,
                installments = installments,
                description = description
            )
            processPayment(payment).fold(
                onSuccess = { transaction ->
                    _instalmentSummary.value = null
                    _instalmentsError.value = null
                    _uiState.value = PaymentUiState.Success(mapper.toUiModel(transaction))
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        message = error.message ?: "Erro desconhecido",
                        isBusinessError = error is BusinessException
                    )
                }
            )
        }
    }

    fun cancelPreviousTransaction(transactionId: String) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = PaymentUiState.Loading
            cancelTransaction(transactionId).fold(
                onSuccess = { transaction ->
                    _uiState.value = PaymentUiState.Success(mapper.toUiModel(transaction))
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        message = error.message ?: "Erro ao cancelar transação",
                        isBusinessError = error is BusinessException
                    )
                }
            )
        }
    }

    fun checkTransactionStatus(transactionId: String) {
        viewModelScope.launch(dispatcher) {
            _uiState.value = PaymentUiState.Loading
            getTransactionStatus(transactionId).fold(
                onSuccess = { transaction ->
                    _uiState.value = PaymentUiState.Success(mapper.toUiModel(transaction))
                },
                onFailure = { error ->
                    _uiState.value = PaymentUiState.Error(
                        message = error.message ?: "Erro ao consultar transação",
                        isBusinessError = error is BusinessException
                    )
                }
            )
        }
    }

    fun resetState() {
        _selectedMethod.value = null
        _instalmentSummary.value = null
        _instalmentsError.value = null
        _instalmentInputVisible.value = false
        _isConfirmEnabled.value = false
        _uiState.value = PaymentUiState.Idle
    }
}