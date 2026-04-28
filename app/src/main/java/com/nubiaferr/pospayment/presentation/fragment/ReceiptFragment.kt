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
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nubiaferr.pospayment.R
import com.nubiaferr.pospayment.databinding.FragmentReceiptBinding
import com.nubiaferr.pospayment.presentation.uistate.PaymentUiState
import com.nubiaferr.pospayment.presentation.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Displays the receipt after a successful transaction.
 *
 * Receives a [TransactionUiModel] via Safe Args — all values are already
 * pre-formatted, so this Fragment only binds strings to views.
 *
 * The "Cancel transaction" button delegates to [PaymentViewModel] and shows
 * a confirmation dialog before proceeding.
 */
@AndroidEntryPoint
class ReceiptFragment : Fragment() {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!

    private val args: ReceiptFragmentArgs by navArgs()

    // Shared ViewModel with PaymentFragment via the NavGraph back stack
    private val viewModel: PaymentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindReceipt()
        setupClickListeners()
        observeUiState()
    }

    private fun bindReceipt() {
        with(args.transaction) {
            binding.tvAmount.text = formattedAmount
            binding.tvMethod.text = methodLabel
            binding.tvStatus.text = statusLabel
            binding.tvAuthCode.text = authCode
            binding.tvDate.text = formattedDate
            binding.tvTransactionId.text = id

            // Hide instalment row when payment is a single charge
            val hasInstalments = instalments.isNotBlank()
            binding.rowInstalments.isVisible = hasInstalments
            binding.dividerInstalments.isVisible = hasInstalments
            if (hasInstalments) binding.tvInstalments.text = instalments
        }
    }

    private fun setupClickListeners() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnNewPayment.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnCancelTransaction.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PaymentUiState.Loading -> showCancelLoading(true)
                        is PaymentUiState.Success -> {
                            // Transaction cancelled — go back to payment screen
                            showCancelLoading(false)
                            findNavController().popBackStack()
                        }
                        is PaymentUiState.Error -> {
                            showCancelLoading(false)
                            showCancelError(state.message)
                        }
                        else -> showCancelLoading(false)
                    }
                }
            }
        }
    }

    private fun showCancelConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_cancel_title))
            .setMessage(getString(R.string.dialog_cancel_message))
            .setPositiveButton(getString(R.string.dialog_cancel_confirm)) { _, _ ->
                viewModel.cancelPreviousTransaction(args.transaction.id)
            }
            .setNegativeButton(getString(R.string.dialog_cancel_dismiss)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showCancelLoading(isLoading: Boolean) {
        binding.btnCancelTransaction.isEnabled = !isLoading
        binding.btnNewPayment.isEnabled = !isLoading
    }

    private fun showCancelError(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_cancel_error_title))
            .setMessage(message)
            .setPositiveButton(getString(R.string.dialog_ok)) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}