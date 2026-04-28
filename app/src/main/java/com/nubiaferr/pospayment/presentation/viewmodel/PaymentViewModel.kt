package com.nubiaferr.pospayment.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nubiaferr.pospayment.R
import com.nubiaferr.pospayment.domain.exception.BusinessException
import com.nubiaferr.pospayment.domain.exception.DebitLimitExceededException
import com.nubiaferr.pospayment.domain.exception.InstalmentNotAllowedException
import com.nubiaferr.pospayment.domain.exception.PixLimitExceededException
import com.nubiaferr.pospayment.domain.exception.VoucherLimitExceededException
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
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    private val _instalmentSummary = MutableStateFlow<String?>(null)
    val instalmentSummary: StateFlow<String?> = _instalmentSummary.asStateFlow()

    private val _instalmentsError = MutableStateFlow<String?>(null)
    val instalmentsError: StateFlow<String?> = _instalmentsError.asStateFlow()

    private val _amountError = MutableStateFlow<String?>(null)
    val amountError: StateFlow<String?> = _amountError.asStateFlow()

    private val _selectedMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedMethod: StateFlow<PaymentMethod?> = _selectedMethod.asStateFlow()

    private val _isConfirmEnabled = MutableStateFlow(false)
    val isConfirmEnabled: StateFlow<Boolean> = _isConfirmEnabled.asStateFlow()

    private val _instalmentInputVisible = MutableStateFlow(false)
    val instalmentInputVisible: StateFlow<Boolean> = _instalmentInputVisible.asStateFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

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
            _isConfirmEnabled.value = evaluateConfirmEnabled(rawAmount, instalmentsError = null)
            return
        }

        val amountResult = validator.validateAmount(rawAmount, _selectedMethod.value)
        if (amountResult !is AmountValidationResult.Valid) {
            _amountError.value = resolveAmountError(amountResult)
            _instalmentsError.value = null
            _instalmentSummary.value = null
            _isConfirmEnabled.value = false
            return
        }
        _amountError.value = null

        val instalmentsResult = validator.validateInstallments(rawInstalments)
        when (instalmentsResult) {
            is InstalmentsValidationResult.ExceedsMax -> {
                _instalmentsError.value = context.getString(
                    R.string.error_instalments_max,
                    instalmentsResult.max
                )
                _instalmentSummary.value = null
            }
            is InstalmentsValidationResult.Valid -> {
                _instalmentsError.value = null
                _instalmentSummary.value = if (rawAmount > 0.0) {
                    val perInstalment = rawAmount / instalmentsResult.installments
                    "${instalmentsResult.installments}x de ${currencyFormatter.format(perInstalment)}"
                } else null
            }
        }

        _isConfirmEnabled.value = evaluateConfirmEnabled(rawAmount, _instalmentsError.value)
    }

    fun processPayment(
        rawAmount: Double,
        method: PaymentMethod,
        rawInstallments: String = "",
        description: String = ""
    ) {
        val amountResult = validator.validateAmount(rawAmount, method)
        val instalmentsResult = validator.validateInstallments(rawInstallments)

        val amountError = if (amountResult !is AmountValidationResult.Valid)
            resolveAmountError(amountResult) else null
        val instalmentsError = if (instalmentsResult is InstalmentsValidationResult.ExceedsMax)
            context.getString(R.string.error_instalments_max, instalmentsResult.max) else null

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
                        message = resolveBusinessError(error),
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
                        message = resolveBusinessError(error),
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
                        message = resolveBusinessError(error),
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
        _amountError.value = null
        _instalmentInputVisible.value = false
        _isConfirmEnabled.value = false
        _uiState.value = PaymentUiState.Idle
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun evaluateConfirmEnabled(rawAmount: Double, instalmentsError: String?): Boolean {
        if (_selectedMethod.value == null) return false
        if (instalmentsError != null) return false
        return validator.validateAmount(rawAmount, _selectedMethod.value) is AmountValidationResult.Valid
    }

    /**
     * Resolves a typed [AmountValidationResult] into a localised string resource.
     */
    private fun resolveAmountError(result: AmountValidationResult): String = when (result) {
        is AmountValidationResult.AmountZeroOrNegative ->
            context.getString(R.string.error_amount_required)
        is AmountValidationResult.ExceedsGlobalMax ->
            context.getString(R.string.error_amount_max, "%.2f".format(result.max))
        is AmountValidationResult.ExceedsMethodLimit ->
            context.getString(
                R.string.error_amount_method_limit,
                context.getString(result.method.labelRes()),
                "%.2f".format(result.limit).replace(".", ",")
            )
        is AmountValidationResult.Valid -> ""
    }

    /**
     * Resolves a [Throwable] into a localised error message.
     * Business exceptions use typed string resources; unknown errors fall back to generic.
     */
    private fun resolveBusinessError(error: Throwable): String = when (error) {
        is InstalmentNotAllowedException ->
            context.getString(R.string.error_instalment_not_allowed, error.minAmount)
        is PixLimitExceededException ->
            context.getString(R.string.error_pix_limit_exceeded, error.limit)
        is DebitLimitExceededException ->
            context.getString(R.string.error_debit_limit_exceeded, error.limit)
        is VoucherLimitExceededException ->
            context.getString(R.string.error_voucher_limit_exceeded, error.limit)
        else -> error.message ?: context.getString(R.string.error_unknown)
    }
}

private fun PaymentMethod.labelRes(): Int = when (this) {
    PaymentMethod.CREDIT  -> R.string.payment_method_credit
    PaymentMethod.DEBIT   -> R.string.payment_method_debit
    PaymentMethod.PIX     -> R.string.payment_method_pix
    PaymentMethod.VOUCHER -> R.string.payment_method_voucher
}