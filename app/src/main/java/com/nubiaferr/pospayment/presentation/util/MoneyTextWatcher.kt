package com.nubiaferr.pospayment.presentation.util

import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.textfield.TextInputEditText
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * A [TextWatcher] that formats a [TextInputEditText] as BRL currency while the
 * operator types, without requiring any special input mask library.
 *
 * Behaviour:
 * - Digits are entered right-to-left like a cash register:
 *   typing "1", "5", "0" produces R$ 0,01 → R$ 0,15 → R$ 1,50.
 * - Always shows two decimal places separated by a comma.
 * - Thousand separators (dots) are inserted automatically.
 * - Non-digit characters are stripped before processing.
 * - Exposes [rawAmount] as a clean [Double] for the ViewModel.
 *
 * The formatting logic lives in [MoneyFormatter], which is pure Kotlin
 * and tested independently without any Android dependency.
 *
 * Usage:
 * ```kotlin
 * val watcher = MoneyTextWatcher(binding.etAmount)
 * binding.etAmount.addTextChangedListener(watcher)
 * // later:
 * viewModel.processPayment(rawAmount = watcher.rawAmount, ...)
 * ```
 */
class MoneyTextWatcher(
    private val editText: TextInputEditText,
    private val formatter: MoneyFormatter = MoneyFormatter()
) : TextWatcher {

    /** The current numeric value as [Double], ready to pass to the ViewModel. */
    var rawAmount: Double = 0.0
        private set

    private var isUpdating = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(editable: Editable?) {
        if (isUpdating) return
        isUpdating = true

        val digits = editable?.toString()
            ?.filter { it.isDigit() }
            ?: ""

        val result = formatter.format(digits)
        rawAmount = result.rawAmount

        editText.setText(result.display)
        editText.setSelection(result.display.length)

        isUpdating = false
    }
}

/**
 * Pure Kotlin formatting logic — no Android dependencies.
 *
 * Separated from [MoneyTextWatcher] so it can be unit-tested
 * without Robolectric or any Android context.
 **/
class MoneyFormatter(locale: Locale = Locale("pt", "BR")) {

    private val symbols = DecimalFormatSymbols(locale)

    data class FormatResult(val display: String, val rawAmount: Double)

    /**
     * Formats a raw digit string into a BRL display string and a parsed [Double].
     *
     * @param digits A string containing only digit characters (non-digits are ignored).
     * @return [FormatResult] with the formatted display string and the numeric value.
     */
    fun format(digits: String): FormatResult {
        val cleaned = digits
            .filter { it.isDigit() }
            .trimStart('0')
            .ifEmpty { "0" }

        val value = cleaned.toBigDecimalOrNull()
            ?.divide(BigDecimal(100), 2, RoundingMode.FLOOR)
            ?: BigDecimal.ZERO

        return FormatResult(
            display = buildDisplay(value),
            rawAmount = value.toDouble()
        )
    }

    private fun buildDisplay(value: BigDecimal): String {
        val plain = value
            .setScale(2, RoundingMode.FLOOR)
            .toPlainString()           // e.g. "1234.56"

        val parts = plain.split(".")
        val intPart = parts[0].trimStart('0').ifEmpty { "0" }
        val decPart = parts.getOrElse(1) { "00" }.padEnd(2, '0')

        val intFormatted = intPart
            .reversed()
            .chunked(3)
            .joinToString(symbols.groupingSeparator.toString())
            .reversed()

        return "R$ $intFormatted${symbols.decimalSeparator}$decPart"
    }
}