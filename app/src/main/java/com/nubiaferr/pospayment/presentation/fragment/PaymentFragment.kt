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
import com.nubiaferr.pospayment.presentation.util.MoneyTextWatcher
import com.nubiaferr.pospayment.presentation.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels()
    private var selectedMethod: PaymentMethod? = null
    private lateinit var moneyWatcher: MoneyTextWatcher

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
        setupMoneyInput()
        observeUiState()
        observeInstalmentSummary()
        setupClickListeners()
    }

    private fun setupMoneyInput() {
        moneyWatcher = MoneyTextWatcher(binding.etAmount)
        binding.etAmount.addTextChangedListener(moneyWatcher)
        binding.etAmount.doAfterTextChanged {
            binding.tilAmount.error = null
            notifyInputChanged()
        }
        binding.etInstallments.doAfterTextChanged {
            binding.tilInstallments.error = null
            notifyInputChanged()
        }
    }

    /**
     * Notifies the ViewModel on every keystroke so [instalmentSummary]
     * stays up to date while the operator types.
     */
    private fun notifyInputChanged() {
        viewModel.onInputChanged(
            rawAmount = moneyWatcher.rawAmount,
            rawInstalments = binding.etInstallments.text?.toString().orEmpty()
        )
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

    /**
     * Shows the per-instalment value below the field in real time.
     * e.g. "12x de R$ 50,00" — visible only when method is Credit and
     * instalment count is greater than 1.
     */
    private fun observeInstalmentSummary() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.instalmentSummary.collect { summary ->
                    binding.tvInstalmentSummary.isVisible = summary != null
                    binding.tvInstalmentSummary.text = summary
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
        if (!isCredit) {
            binding.etInstallments.text?.clear()
            binding.tvInstalmentSummary.isVisible = false
        }

        binding.btnConfirm.isVisible = true
    }

    private fun submitPayment() {
        val method = selectedMethod ?: return
        viewModel.processPayment(
            rawAmount = moneyWatcher.rawAmount,
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
        binding.tilAmount.error = state.amountError
        binding.tilInstallments.error = state.instalmentsError
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

    private fun clearInputs() {
        selectedMethod = null
        binding.etAmount.text?.clear()
        binding.etInstallments.text?.clear()
        binding.tilAmount.error = null
        binding.tilInstallments.error = null
        binding.tvInstalmentSummary.isVisible = false
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