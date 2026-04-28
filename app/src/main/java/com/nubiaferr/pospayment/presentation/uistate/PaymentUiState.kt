package com.nubiaferr.pospayment.presentation.uistate

import com.nubiaferr.pospayment.presentation.model.TransactionUiModel

sealed class PaymentUiState {

    object Idle : PaymentUiState()

    object Loading : PaymentUiState()

    object AwaitingCard : PaymentUiState()

    /**
     * Raw input failed validation before any network call.
     * Each field has its own nullable message so the Fragment can show
     * errors independently without clearing the other field.
     *
     * @property amountError    Error message for the amount field, or null if valid.
     * @property instalmentsError Error message for the instalments field, or null if valid.
     */
    data class ValidationError(
        val amountError: String? = null,
        val instalmentsError: String? = null
    ) : PaymentUiState()

    data class Success(val transaction: TransactionUiModel) : PaymentUiState()

    data class Error(
        val message: String,
        val isBusinessError: Boolean = false
    ) : PaymentUiState()
}