package com.nubiaferr.pospayment.presentation.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.nubiaferr.pospayment.databinding.FragmentReceiptBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Displays the receipt after a successful transaction.
 *
 * Receives a [TransactionUiModel] via Safe Args —
 * all values are already formatted, so this Fragment only binds strings to views.
 */
@AndroidEntryPoint
class ReceiptFragment : Fragment() {

    private var _binding: FragmentReceiptBinding? = null
    private val binding get() = _binding!!

    private val args: ReceiptFragmentArgs by navArgs()

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
    }

    private fun bindReceipt() {
        with(args.transaction) {
            binding.tvAmount.text = formattedAmount
            binding.tvMethod.text = methodLabel
            binding.tvStatus.text = statusLabel
            binding.tvAuthCode.text = authCode
            binding.tvDate.text = formattedDate
            binding.tvTransactionId.text = id

            // Hide instalment row when payment is single charge
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
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}