package com.nubiaferr.pospayment.presentation.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
 * Responsibilities (and only these):
 * - Render each [PaymentUiState] emitted by [PaymentViewModel].
 * - Forward user interactions (button taps, input values) to the ViewModel.
 * - Navigate to [ReceiptFragment] on success.
 *
 * No business logic lives here.
 */
@AndroidEntryPoint
class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels()

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
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PaymentUiState.Idle -> showIdle()
                        is PaymentUiState.Loading -> showLoading()
                        is PaymentUiState.AwaitingCard -> showAwaitingCard()
                        is PaymentUiState.Success -> navigateToReceipt(state)
                        is PaymentUiState.Error -> showError(state)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnCredit.setOnClickListener { submitPayment(PaymentMethod.CREDIT) }
        binding.btnDebit.setOnClickListener { submitPayment(PaymentMethod.DEBIT) }
        binding.btnPix.setOnClickListener { submitPayment(PaymentMethod.PIX) }
        binding.btnVoucher.setOnClickListener { submitPayment(PaymentMethod.VOUCHER) }
        binding.btnRetry.setOnClickListener { viewModel.resetState() }
    }

    private fun submitPayment(method: PaymentMethod) {
        val amountText = binding.etAmount.text?.toString()
        val amount = amountText?.toDoubleOrNull() ?: return
        val installments = binding.etInstallments.text?.toString()?.toIntOrNull() ?: 1
        viewModel.processPayment(
            amount = amount,
            method = method,
            installments = installments
        )
    }

    private fun showIdle() {
        binding.progressBar.isVisible = false
        binding.groupPaymentButtons.isVisible = true
        binding.groupError.isVisible = false
        binding.tvAwaitingCard.isVisible = false
    }

    private fun showLoading() {
        binding.progressBar.isVisible = true
        binding.groupPaymentButtons.isVisible = false
        binding.groupError.isVisible = false
        binding.tvAwaitingCard.isVisible = false
    }

    private fun showAwaitingCard() {
        binding.progressBar.isVisible = false
        binding.tvAwaitingCard.isVisible = true
        binding.groupPaymentButtons.isVisible = false
    }

    private fun showError(state: PaymentUiState.Error) {
        binding.progressBar.isVisible = false
        binding.groupError.isVisible = true
        binding.tvErrorMessage.text = state.message
        binding.groupPaymentButtons.isVisible = !state.isBusinessError
    }

    private fun navigateToReceipt(state: PaymentUiState.Success) {
        val action = PaymentFragmentDirections
            .actionPaymentFragmentToReceiptFragment(state.transaction)
        findNavController().navigate(action)
        viewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}