package com.nubiaferr.pospayment.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MoneyFormatter].
 *
 * Tests the formatting logic directly — no Android context, no Robolectric,
 * no theme required. [MoneyTextWatcher] is a thin wrapper that just calls
 * [MoneyFormatter.format] and binds the result to the EditText.
 */
class MoneyFormatterTest {

    private lateinit var formatter: MoneyFormatter

    @Before
    fun setUp() {
        formatter = MoneyFormatter()
    }

    // ── rawAmount ──────────────────────────────────────────────────────────────

    @Test
    fun `given empty string, rawAmount is zero`() {
        assertEquals(0.0, formatter.format("").rawAmount, 0.001)
    }

    @Test
    fun `given digits 150, rawAmount is 1_50`() {
        assertEquals(1.50, formatter.format("150").rawAmount, 0.001)
    }

    @Test
    fun `given digits 10000, rawAmount is 100_00`() {
        assertEquals(100.00, formatter.format("10000").rawAmount, 0.001)
    }

    @Test
    fun `given digits 123456, rawAmount is 1234_56`() {
        assertEquals(1234.56, formatter.format("123456").rawAmount, 0.001)
    }

    @Test
    fun `given digits 9999999, rawAmount is 99999_99`() {
        assertEquals(99999.99, formatter.format("9999999").rawAmount, 0.001)
    }

    // ── display string ─────────────────────────────────────────────────────────

    @Test
    fun `given empty string, display is R$ 0,00`() {
        assertEquals("R$ 0,00", formatter.format("").display)
    }

    @Test
    fun `given single digit 5, display is R$ 0,05`() {
        assertEquals("R$ 0,05", formatter.format("5").display)
    }

    @Test
    fun `given digits 150, display is R$ 1,50`() {
        assertEquals("R$ 1,50", formatter.format("150").display)
    }

    @Test
    fun `given digits 100000, display has thousand separator`() {
        assertEquals("R$ 1.000,00", formatter.format("100000").display)
    }

    @Test
    fun `given digits 9999999, display is R$ 99_999,99`() {
        assertEquals("R$ 99.999,99", formatter.format("9999999").display)
    }

    @Test
    fun `given digits with leading zeros, display strips them correctly`() {
        assertEquals("R$ 0,05", formatter.format("005").display)
    }

    // ── non-digit input ────────────────────────────────────────────────────────

    @Test
    fun `given string with non-digit chars, non-digits are ignored`() {
        assertEquals(
            formatter.format("150").display,
            formatter.format("R$ 1,50").display
        )
    }
}