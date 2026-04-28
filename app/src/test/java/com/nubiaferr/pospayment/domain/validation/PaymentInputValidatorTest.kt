package com.nubiaferr.pospayment.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PaymentInputValidator].
 *
 * Amount now arrives as a pre-parsed [Double] from [MoneyTextWatcher].
 * These tests cover boundary rules only — string parsing is tested in [MoneyTextWatcherTest].
 */
class PaymentInputValidatorTest {

    private lateinit var validator: PaymentInputValidator

    @Before
    fun setUp() {
        validator = PaymentInputValidator()
    }

    // ── validateAmount — valid ─────────────────────────────────────────────────

    @Test
    fun `given positive amount, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount(29.90)
        assertTrue(result is AmountValidationResult.Valid)
        assertEquals(29.90, (result as AmountValidationResult.Valid).amount, 0.001)
    }

    @Test
    fun `given minimum positive amount, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount(0.01)
        assertTrue(result is AmountValidationResult.Valid)
    }

    @Test
    fun `given amount exactly at maximum, when validateAmount, then returns Valid`() {
        val result = validator.validateAmount(99999.99)
        assertTrue(result is AmountValidationResult.Valid)
    }

    @Test
    fun `given typical amount, when validateAmount, then Valid preserves the exact value`() {
        val result = validator.validateAmount(150.0)
        assertEquals(150.0, (result as AmountValidationResult.Valid).amount, 0.001)
    }

    // ── validateAmount — invalid ───────────────────────────────────────────────

    @Test
    fun `given zero, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount(0.0)
        assertTrue(result is AmountValidationResult.Invalid)
        assertTrue((result as AmountValidationResult.Invalid).message.contains("maior que zero"))
    }

    @Test
    fun `given negative value, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount(-10.0)
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given amount one cent above maximum, when validateAmount, then returns Invalid`() {
        val result = validator.validateAmount(100000.00)
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given amount well above maximum, when validateAmount, then Invalid message is not blank`() {
        val result = validator.validateAmount(999999.0)
        assertTrue((result as AmountValidationResult.Invalid).message.isNotBlank())
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
    fun `given zero, when validateInstallments, then returns 1`() {
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