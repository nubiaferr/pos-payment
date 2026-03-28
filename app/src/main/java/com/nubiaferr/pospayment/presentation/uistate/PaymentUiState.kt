package com.nubiaferr.pospayment.presentation.uistate

import com.nubiaferr.pospayment.presentation.model.TransactionUiModel

/**
 * Represents every possible state the payment screen can be in.
 *
 * The [PaymentViewModel] emits these via
 * `StateFlow<PaymentUiState>`. The Fragment collects and renders each state
 * without any business logic of its own.
 */
sealed class PaymentUiState {

    /** Initial state — no action taken yet. */
    object Idle : PaymentUiState()

    /** A network or terminal operation is in progress. */
    object Loading : PaymentUiState()

    /**
     * The terminal is waiting for the customer to insert, tap or swipe their card.
     * Only applicable for [PaymentMethod.CREDIT] and
     * [PaymentMethod.DEBIT].
     */
    object AwaitingCard : PaymentUiState()

    /**
     * The payment was authorised by the acquirer.
     *
     * @property transaction Display model ready for the receipt screen.
     */
    data class Success(val transaction: TransactionUiModel) : PaymentUiState()

    /**
     * An error occurred during processing.
     *
     * @property message Human-readable error message to display to the operator.
     * @property isBusinessError `true` for domain rule violations (e.g. instalment limit),
     *                           `false` for network/infrastructure errors.
     */
    data class Error(
        val message: String,
        val isBusinessError: Boolean = false
    ) : PaymentUiState()
}