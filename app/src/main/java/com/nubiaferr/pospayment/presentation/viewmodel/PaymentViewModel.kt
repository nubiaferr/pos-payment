package com.nubiaferr.pospayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nubiaferr.pospayment.domain.exception.BusinessException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
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
     * Instalment summary shown below the field while the operator fills in the form.
     * Emits a formatted string like "12x de R$ 50,00" or null when not applicable.
     */
    private val _instalmentSummary = MutableStateFlow<String?>(null)
    val instalmentSummary: StateFlow<String?> = _instalmentSummary.asStateFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /**
     * Called on every keystroke in either the amount or instalments field.
     * Updates [instalmentSummary] in real time so the operator sees the
     * per-instalment value before tapping Confirm.
     *
     * @param rawAmount    Pre-parsed [Double] from [MoneyTextWatcher.rawAmount].
     * @param rawInstalments Raw string from the instalments field.
     */
    fun onInputChanged(rawAmount: Double, rawInstalments: String) {
        val instalments = rawInstalments.trim().toIntOrNull() ?: 1
        _instalmentSummary.value = if (instalments > 1 && rawAmount > 0.0) {
            val perInstalment = rawAmount / instalments
            "${instalments}x de ${currencyFormatter.format(perInstalment)}"
        } else {
            null
        }
    }

    /**
     * Validates both fields and, if valid, initiates a payment.
     * Both field errors are collected before emitting so the operator
     * sees all problems at once instead of one at a time.
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
        _instalmentSummary.value = null
        _uiState.value = PaymentUiState.Idle
    }
}