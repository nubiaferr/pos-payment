package com.nubiaferr.pospayment.presentation.uistate

import com.nubiaferr.pospayment.domain.exception.BusinessException
import com.nubiaferr.pospayment.domain.validation.AmountValidationResult
import com.nubiaferr.pospayment.domain.validation.InstalmentsValidationResult
import com.nubiaferr.pospayment.presentation.model.TransactionUiModel

/**
 * Every possible state the payment screen can be in.
 */
sealed class PaymentUiState {

    object Idle : PaymentUiState()

    object Loading : PaymentUiState()

    /**
     * Raw input failed validation before any network call.
     * @property amountError      Typed amount validation failure, or null if valid.
     * @property instalmentsError Typed instalment validation failure, or null if valid.
     */
    data class ValidationError(
        val amountError: AmountValidationResult? = null,
        val instalmentsError: InstalmentsValidationResult? = null
    ) : PaymentUiState()

    /** Payment was authorized by the acquirer. */
    data class Success(val transaction: TransactionUiModel) : PaymentUiState()

    /**
     * A network or business error occurred after submit.
     *
     * @property error           The original throwable — the Fragment resolves the message.
     * @property isBusinessError True for [BusinessException] subclasses.
     */
    data class Error(
        val error: Throwable,
        val isBusinessError: Boolean
    ) : PaymentUiState()
}