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
 * Delegates input validation to [PaymentInputValidator] and business
 * operations to the domain use cases. The Fragment passes the pre-parsed
 * [Double] from [MoneyTextWatcher] — no string parsing happens here.
 *
 * @property processPayment       Use case for initiating a payment.
 * @property cancelTransaction    Use case for reversing an approved transaction.
 * @property getTransactionStatus Use case for polling a pending transaction.
 * @property mapper               Converts domain entities to UI models.
 * @property validator            Enforces boundary rules on the parsed input.
 * @property dispatcher           Coroutine dispatcher — injectable for testing.
 */
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
     * Validates the pre-parsed amount and, if valid, initiates a payment.
     *
     * Emits [PaymentUiState.ValidationError] synchronously for boundary violations
     * (zero, negative, above maximum) so the Fragment can show a field-level error
     * without triggering a network call.
     *
     * @param rawAmount       Pre-parsed [Double] from [MoneyTextWatcher.rawAmount].
     * @param method          Payment method selected by the operator.
     * @param rawInstallments Raw string from the instalment field (may be blank).
     * @param description     Optional merchant description.
     */
    fun processPayment(
        rawAmount: Double,
        method: PaymentMethod,
        rawInstallments: String = "",
        description: String = ""
    ) {
        val amountResult = validator.validateAmount(rawAmount)
        if (amountResult is AmountValidationResult.Invalid) {
            _uiState.value = PaymentUiState.ValidationError(amountResult.message)
            return
        }

        val amount = (amountResult as AmountValidationResult.Valid).amount
        val installments = validator.validateInstallments(rawInstallments)

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
        _uiState.value = PaymentUiState.Idle
    }
}