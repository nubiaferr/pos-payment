package com.nubiaferr.pospayment.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PaymentInputValidator].
 *
 * Covers every branch of amount validation and instalment sanitisation.
 * No mocks needed — pure logic.
 */
class PaymentInputValidatorTest {

    private lateinit var validator: PaymentInputValidator

    @Before
    fun setUp() {
        validator = PaymentInputValidator()
    }

    // ── validateAmount — valid inputs ──────────────────────────────────────────

    @Test
    fun `given valid decimal with dot, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount("29.90")
        assertTrue(result is AmountValidationResult.Valid)
        assertEquals(29.90, (result as AmountValidationResult.Valid).amount, 0.001)
    }

    @Test
    fun `given valid decimal with comma, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount("29,90")
        assertTrue(result is AmountValidationResult.Valid)
        assertEquals(29.90, (result as AmountValidationResult.Valid).amount, 0.001)
    }

    @Test
    fun `given integer string, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount("100")
        assertTrue(result is AmountValidationResult.Valid)
        assertEquals(100.0, (result as AmountValidationResult.Valid).amount, 0.001)
    }

    @Test
    fun `given amount with leading and trailing spaces, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount("  50.00  ")
        assertTrue(result is AmountValidationResult.Valid)
    }

    @Test
    fun `given amount exactly at maximum, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount("99999.99")
        assertTrue(result is AmountValidationResult.Valid)
    }

    // ── validateAmount — invalid inputs ───────────────────────────────────────

    @Test
    fun `given blank string, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("")
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given whitespace only, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("   ")
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given non-numeric string, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("abc")
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given zero, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("0")
        assertTrue(result is AmountValidationResult.Invalid)
        assertTrue((result as AmountValidationResult.Invalid).message.contains("maior que zero"))
    }

    @Test
    fun `given negative value, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("-10.00")
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given amount above maximum, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("100000.00")
        assertTrue(result is AmountValidationResult.Invalid)
        assertTrue((result as AmountValidationResult.Invalid).message.isNotBlank())
    }

    @Test
    fun `given amount one cent above maximum, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("100000.00")
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given malformed double, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount("1.5.3")
        assertTrue(result is AmountValidationResult.Invalid)
    }

    // ── validateInstallments ───────────────────────────────────────────────────

    @Test
    fun `given valid installments string, when validateInstallments, then returns parsed value`() {
        assertEquals(3, validator.validateInstallments("3"))
    }

    @Test
    fun `given blank string, when validateInstallments, then returns 1`() {
        assertEquals(1, validator.validateInstallments(""))
    }

    @Test
    fun `given non-numeric string, when validateInstallments, then returns 1`() {
        assertEquals(1, validator.validateInstallments("abc"))
    }

    @Test
    fun `given zero, when validateInstallments, then returns 1 (clamped to minimum)`() {
        assertEquals(1, validator.validateInstallments("0"))
    }

    @Test
    fun `given value above maximum, when validateInstallments, then returns MAX_INSTALLMENTS`() {
        assertEquals(
            PaymentInputValidator.MAX_INSTALLMENTS,
            validator.validateInstallments("99")
        )
    }

    @Test
    fun `given exactly MAX_INSTALLMENTS, when validateInstallments, then returns MAX_INSTALLMENTS`() {
        assertEquals(
            PaymentInputValidator.MAX_INSTALLMENTS,
            validator.validateInstallments(PaymentInputValidator.MAX_INSTALLMENTS.toString())
        )
    }
}