package com.nubiaferr.pospayment.domain.validation

import com.nubiaferr.pospayment.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [PaymentInputValidator].
 *
 * Verifies typed result variants — message resolution is tested via
 * PaymentViewModelTest (which has Android context via the ViewModel).
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
    fun `given amount above global maximum, when validateAmount, then returns ExceedsGlobalMax`() {
        assertTrue(validator.validateAmount(100000.0) is AmountValidationResult.ExceedsGlobalMax)
    }

    @Test
    fun `given ExceedsGlobalMax, then max field contains the configured limit`() {
        val result = validator.validateAmount(100000.0) as AmountValidationResult.ExceedsGlobalMax
        assertEquals(99999.99, result.max, 0.001)
    }

    // ── validateAmount — per-method limits ────────────────────────────────────

    @Test
    fun `given pix amount above limit, when validateAmount with PIX, then returns ExceedsMethodLimit`() {
        val result = validator.validateAmount(50_000.01, PaymentMethod.PIX)
        assertTrue(result is AmountValidationResult.ExceedsMethodLimit)
    }

    @Test
    fun `given pix amount exactly at limit, when validateAmount with PIX, then returns Valid`() {
        assertTrue(validator.validateAmount(50_000.0, PaymentMethod.PIX) is AmountValidationResult.Valid)
    }

    @Test
    fun `given ExceedsMethodLimit for PIX, then method and limit fields are correct`() {
        val result = validator.validateAmount(60_000.0, PaymentMethod.PIX) as AmountValidationResult.ExceedsMethodLimit
        assertEquals(PaymentMethod.PIX, result.method)
        assertEquals(50_000.0, result.limit, 0.001)
    }

    @Test
    fun `given debit amount above limit, when validateAmount with DEBIT, then returns ExceedsMethodLimit`() {
        val result = validator.validateAmount(10_000.01, PaymentMethod.DEBIT)
        assertTrue(result is AmountValidationResult.ExceedsMethodLimit)
    }

    @Test
    fun `given debit amount exactly at limit, when validateAmount with DEBIT, then returns Valid`() {
        assertTrue(validator.validateAmount(10_000.0, PaymentMethod.DEBIT) is AmountValidationResult.Valid)
    }

    @Test
    fun `given voucher amount above limit, when validateAmount with VOUCHER, then returns ExceedsMethodLimit`() {
        val result = validator.validateAmount(1_000.01, PaymentMethod.VOUCHER)
        assertTrue(result is AmountValidationResult.ExceedsMethodLimit)
    }

    @Test
    fun `given voucher amount exactly at limit, when validateAmount with VOUCHER, then returns Valid`() {
        assertTrue(validator.validateAmount(1_000.0, PaymentMethod.VOUCHER) is AmountValidationResult.Valid)
    }

    @Test
    fun `given credit amount within global limit, when validateAmount with CREDIT, then returns Valid`() {
        assertTrue(validator.validateAmount(99999.99, PaymentMethod.CREDIT) is AmountValidationResult.Valid)
    }

    @Test
    fun `given no method, when validateAmount with valid amount, then returns Valid`() {
        assertTrue(validator.validateAmount(100.0, null) is AmountValidationResult.Valid)
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