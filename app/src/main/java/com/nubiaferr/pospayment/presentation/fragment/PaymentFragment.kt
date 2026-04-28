package com.nubiaferr.pospayment.presentation.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.nubiaferr.pospayment.databinding.FragmentPaymentBinding
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.presentation.uistate.PaymentUiState
import com.nubiaferr.pospayment.presentation.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Entry screen for the POS payment flow.
 *
 * Two-step UX:
 * 1. Operator enters the amount and taps a payment method button to select it.
 *    Selecting Credit reveals the instalment field. No payment is triggered yet.
 * 2. Operator taps "Confirmar pagamento" to submit.
 *
 * This Fragment contains zero validation logic or business constants —
 * it passes raw strings to [PaymentViewModel] and renders whatever state it receives.
 */
@AndroidEntryPoint
class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels()

    private var selectedMethod: PaymentMethod? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeUiState()
        setupClickListeners()
        setupInputValidation()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PaymentUiState.Idle            -> showIdle()
                        is PaymentUiState.Loading         -> showLoading()
                        is PaymentUiState.AwaitingCard    -> showAwaitingCard()
                        is PaymentUiState.ValidationError -> showValidationError(state)
                        is PaymentUiState.Success         -> navigateToReceipt(state)
                        is PaymentUiState.Error           -> showError(state)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCredit.setOnClickListener  { selectMethod(PaymentMethod.CREDIT) }
        binding.btnDebit.setOnClickListener   { selectMethod(PaymentMethod.DEBIT) }
        binding.btnPix.setOnClickListener     { selectMethod(PaymentMethod.PIX) }
        binding.btnVoucher.setOnClickListener { selectMethod(PaymentMethod.VOUCHER) }

        binding.btnConfirm.setOnClickListener { submitPayment() }

        binding.btnRetry.setOnClickListener {
            clearInputs()
            viewModel.resetState()
        }
    }

    private fun setupInputValidation() {
        // Clear field error as soon as the user starts editing
        binding.etAmount.doAfterTextChanged {
            binding.tilAmount.error = null
        }
    }

    private fun selectMethod(method: PaymentMethod) {
        selectedMethod = method

        listOf(
            PaymentMethod.CREDIT  to binding.btnCredit,
            PaymentMethod.DEBIT   to binding.btnDebit,
            PaymentMethod.PIX     to binding.btnPix,
            PaymentMethod.VOUCHER to binding.btnVoucher,
        ).forEach { (m, btn) -> btn.isSelected = (m == method) }

        val isCredit = method == PaymentMethod.CREDIT
        binding.layoutInstallments.isVisible = isCredit
        if (!isCredit) binding.etInstallments.text?.clear()

        binding.btnConfirm.isVisible = true
    }

    /**
     * Passes raw strings directly to the ViewModel — no parsing or validation here.
     */
    private fun submitPayment() {
        val method = selectedMethod ?: return
        viewModel.processPayment(
            rawAmount = binding.etAmount.text?.toString().orEmpty(),
            method = method,
            rawInstallments = binding.etInstallments.text?.toString().orEmpty()
        )
        binding.btnConfirm.isVisible = false
    }

    // ── State renderers ────────────────────────────────────────────────────────

    private fun showIdle() {
        binding.cardAmount.isVisible = true
        binding.groupPaymentButtons.isVisible = true
        binding.layoutLoading.isVisible = false
        binding.cardError.isVisible = false
    }

    private fun showLoading() {
        binding.tvLoadingMessage.text = "Processando pagamento…"
        binding.layoutLoading.isVisible = true
        binding.cardAmount.isVisible = false
        binding.groupPaymentButtons.isVisible = false
        binding.cardError.isVisible = false
    }

    private fun showAwaitingCard() {
        binding.tvLoadingMessage.text = "Aguardando o cartão…"
        binding.layoutLoading.isVisible = true
        binding.cardAmount.isVisible = false
        binding.groupPaymentButtons.isVisible = false
    }

    private fun showValidationError(state: PaymentUiState.ValidationError) {
        // Field-level error — keep the form visible so the operator can correct it
        binding.tilAmount.error = state.message
        binding.btnConfirm.isVisible = true
        binding.layoutLoading.isVisible = false
    }

    private fun showError(state: PaymentUiState.Error) {
        binding.layoutLoading.isVisible = false
        binding.cardError.isVisible = true
        binding.tvErrorMessage.text = state.message
        binding.groupPaymentButtons.isVisible = !state.isBusinessError
        binding.cardAmount.isVisible = !state.isBusinessError
    }

    private fun navigateToReceipt(state: PaymentUiState.Success) {
        val action = PaymentFragmentDirections
            .actionPaymentFragmentToReceiptFragment(state.transaction)
        findNavController().navigate(action)
        viewModel.resetState()
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun clearInputs() {
        selectedMethod = null
        binding.etAmount.text?.clear()
        binding.etInstallments.text?.clear()
        binding.tilAmount.error = null
        binding.layoutInstallments.isVisible = false
        binding.btnConfirm.isVisible = false
        listOf(
            binding.btnCredit,
            binding.btnDebit,
            binding.btnPix,
            binding.btnVoucher
        ).forEach { it.isSelected = false }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}