package com.nubiaferr.pospayment.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nubiaferr.pospayment.domain.exception.BusinessException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.usecase.CancelTransactionUseCase
import com.nubiaferr.pospayment.domain.usecase.GetTransactionStatusUseCase
import com.nubiaferr.pospayment.domain.usecase.ProcessPaymentUseCase
import com.nubiaferr.pospayment.presentation.mapper.PaymentUiMapper
import com.nubiaferr.pospayment.presentation.uistate.PaymentUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the payment screen.
 *
 * Owns the UI state lifecycle and delegates all business operations to the
 * domain use cases. The Fragment observes [uiState] and reacts to each emission
 * without containing any logic itself.
 *
 * @property processPayment         Use case for initiating a payment.
 * @property cancelTransaction      Use case for reversing an approved transaction.
 * @property getTransactionStatus   Use case for polling a pending transaction.
 * @property mapper                 Converts domain entities to UI models.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val processPayment: ProcessPaymentUseCase,
    private val cancelTransaction: CancelTransactionUseCase,
    private val getTransactionStatus: GetTransactionStatusUseCase,
    private val mapper: PaymentUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)

    /**
     * The current UI state. Collect this in the Fragment using
     * `collectLatestWithLifecycle` to automatically respect the lifecycle.
     */
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    /**
     * Initiates a payment with the given parameters.
     *
     * Emits [PaymentUiState.Loading] immediately, then either
     * [PaymentUiState.Success] or [PaymentUiState.Error].
     *
     * @param amount       Transaction amount in BRL.
     * @param method       Payment method selected by the customer.
     * @param installments Number of instalments (credit only, defaults to 1).
     * @param description  Optional merchant description.
     */
    fun processPayment(
        amount: Double,
        method: PaymentMethod,
        installments: Int = 1,
        description: String = ""
    ) {
        viewModelScope.launch {
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

    /**
     * Cancels a previously approved transaction.
     *
     * @param transactionId The unique identifier of the transaction to reverse.
     */
    fun cancelPreviousTransaction(transactionId: String) {
        viewModelScope.launch {
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

    /**
     * Checks the current status of a transaction.
     * Useful for recovering from a terminal crash or polling a pending Pix.
     *
     * @param transactionId The unique identifier of the transaction.
     */
    fun checkTransactionStatus(transactionId: String) {
        viewModelScope.launch {
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

    /** Resets the UI state back to [PaymentUiState.Idle]. */
    fun resetState() {
        _uiState.value = PaymentUiState.Idle
    }
}