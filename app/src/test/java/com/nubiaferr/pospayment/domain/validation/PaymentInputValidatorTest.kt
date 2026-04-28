package com.nubiaferr.pospayment.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PaymentInputValidator].
 *
 * Covers global boundary rules only (zero, negative, global maximum, instalment count).
 * Per-method transaction limits are covered in the strategy tests
 * (DebitPaymentStrategyTest, PixPaymentStrategyTest, VoucherPaymentStrategyTest).
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
        assertTrue(validator.validateAmount(29.90) is AmountValidationResult.Valid)
    }

    @Test
    fun `given minimum positive amount, when validateAmount, then returns Valid`() {
        assertTrue(validator.validateAmount(0.01) is AmountValidationResult.Valid)
    }

    @Test
    fun `given amount exactly at global maximum, when validateAmount, then returns Valid`() {
        assertTrue(validator.validateAmount(99999.99) is AmountValidationResult.Valid)
    }

    @Test
    fun `given valid amount, when validateAmount, then Valid preserves the exact value`() {
        val result = validator.validateAmount(150.0) as AmountValidationResult.Valid
        assertEquals(150.0, result.amount, 0.001)
    }

    // ── validateAmount — zero / negative ──────────────────────────────────────

    @Test
    fun `given zero, when validateAmount, then returns AmountZeroOrNegative`() {
        assertTrue(validator.validateAmount(0.0) is AmountValidationResult.AmountZeroOrNegative)
    }

    @Test
    fun `given negative value, when validateAmount, then returns AmountZeroOrNegative`() {
        assertTrue(validator.validateAmount(-10.0) is AmountValidationResult.AmountZeroOrNegative)
    }

    // ── validateAmount — global max ────────────────────────────────────────────

    @Test
    fun `given amount one cent above global maximum, when validateAmount, then returns ExceedsGlobalMax`() {
        assertTrue(validator.validateAmount(100000.0) is AmountValidationResult.ExceedsGlobalMax)
    }

    @Test
    fun `given ExceedsGlobalMax, then max field matches configured limit`() {
        val result = validator.validateAmount(100000.0) as AmountValidationResult.ExceedsGlobalMax
        assertEquals(99999.99, result.max, 0.001)
    }

    // ── validateInstallments ───────────────────────────────────────────────────

    @Test
    fun `given valid count within limit, when validateInstallments, then returns Valid`() {
        val result = validator.validateInstallments("6") as InstalmentsValidationResult.Valid
        assertEquals(6, result.installments)
    }

    @Test
    fun `given blank string, when validateInstallments, then returns Valid with default 1`() {
        val result = validator.validateInstallments("") as InstalmentsValidationResult.Valid
        assertEquals(1, result.installments)
    }

    @Test
    fun `given non-numeric string, when validateInstallments, then returns Valid with default 1`() {
        val result = validator.validateInstallments("abc") as InstalmentsValidationResult.Valid
        assertEquals(1, result.installments)
    }

    @Test
    fun `given zero, when validateInstallments, then returns Valid with 1`() {
        val result = validator.validateInstallments("0") as InstalmentsValidationResult.Valid
        assertEquals(1, result.installments)
    }

    @Test
    fun `given exactly MAX_INSTALLMENTS, when validateInstallments, then returns Valid`() {
        val result = validator.validateInstallments("12") as InstalmentsValidationResult.Valid
        assertEquals(12, result.installments)
    }

    @Test
    fun `given count above maximum, when validateInstallments, then returns ExceedsMax`() {
        assertTrue(validator.validateInstallments("13") is InstalmentsValidationResult.ExceedsMax)
    }

    @Test
    fun `given ExceedsMax, then max field contains the configured limit`() {
        val result = validator.validateInstallments("600") as InstalmentsValidationResult.ExceedsMax
        assertEquals(12, result.max)
    }

    @Test
    fun `given count above maximum, when validateInstallments, then does NOT silently clamp`() {
        assertTrue(validator.validateInstallments("600") is InstalmentsValidationResult.ExceedsMax)
    }
}