package com.nubiaferr.pospayment.domain.validation

import com.nubiaferr.pospayment.domain.model.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PaymentInputValidatorTest {

    private lateinit var validator: PaymentInputValidator

    @Before
    fun setUp() {
        validator = PaymentInputValidator()
    }

    // ── validateAmount ─────────────────────────────────────────────────────────

    @Test
    fun `given positive amount, when validateAmount, then returns Valid`() {
        assertTrue(validator.validateAmount(29.90) is AmountValidationResult.Valid)
    }

    @Test
    fun `given minimum positive amount, when validateAmount, then returns Valid`() {
        assertTrue(validator.validateAmount(0.01) is AmountValidationResult.Valid)
    }

    @Test
    fun `given amount exactly at maximum, when validateAmount, then returns Valid`() {
        assertTrue(validator.validateAmount(99999.99) is AmountValidationResult.Valid)
    }

    @Test
    fun `given zero, when validateAmount, then returns Invalid with correct message`() {
        val result = validator.validateAmount(0.0) as AmountValidationResult.Invalid
        assertTrue(result.message.contains("maior que zero"))
    }

    @Test
    fun `given negative value, when validateAmount, then returns Invalid`() {
        assertTrue(validator.validateAmount(-10.0) is AmountValidationResult.Invalid)
    }

    @Test
    fun `given amount above maximum, when validateAmount, then returns Invalid`() {
        assertTrue(validator.validateAmount(100000.0) is AmountValidationResult.Invalid)
    }

    // ── validateInstallments ───────────────────────────────────────────────────

    @Test
    fun `given valid count within limit, when validateInstallments, then returns Valid with parsed value`() {
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
    fun `given count above maximum, when validateInstallments, then returns Invalid`() {
        assertTrue(validator.validateInstallments("13") is InstalmentsValidationResult.Invalid)
    }

    @Test
    fun `given count well above maximum, when validateInstallments, then Invalid message contains limit`() {
        val result = validator.validateInstallments("600") as InstalmentsValidationResult.Invalid
        assertTrue(result.message.contains("12"))
    }

    @Test
    fun `given count above maximum, when validateInstallments, then does NOT silently clamp`() {
        assertTrue(validator.validateInstallments("600") is InstalmentsValidationResult.Invalid)
    }

// ── validateAmount — per-method limits ────────────────────────────────────

    @Test
    fun `given pix amount above limit, when validateAmount with PIX, then returns Invalid`() {
        val result = validator.validateAmount(50_000.01, PaymentMethod.PIX)
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given pix amount exactly at limit, when validateAmount with PIX, then returns Valid`() {
        val result = validator.validateAmount(50_000.0, PaymentMethod.PIX)
        assertTrue(result is AmountValidationResult.Valid)
    }

    @Test
    fun `given debit amount above limit, when validateAmount with DEBIT, then returns Invalid`() {
        val result = validator.validateAmount(10_000.01, PaymentMethod.DEBIT)
        assertTrue(result is AmountValidationResult.Invalid)
        assertTrue((result as AmountValidationResult.Invalid).message.contains("Débito"))
    }

    @Test
    fun `given debit amount exactly at limit, when validateAmount with DEBIT, then returns Valid`() {
        val result = validator.validateAmount(10_000.0, PaymentMethod.DEBIT)
        assertTrue(result is AmountValidationResult.Valid)
    }

    @Test
    fun `given voucher amount above limit, when validateAmount with VOUCHER, then returns Invalid`() {
        val result = validator.validateAmount(1_000.01, PaymentMethod.VOUCHER)
        assertTrue(result is AmountValidationResult.Invalid)
        assertTrue((result as AmountValidationResult.Invalid).message.contains("Voucher"))
    }

    @Test
    fun `given voucher amount exactly at limit, when validateAmount with VOUCHER, then returns Valid`() {
        val result = validator.validateAmount(1_000.0, PaymentMethod.VOUCHER)
        assertTrue(result is AmountValidationResult.Valid)
    }

    @Test
    fun `given credit amount above global max, when validateAmount with CREDIT, then returns Invalid`() {
        val result = validator.validateAmount(100_000.0, PaymentMethod.CREDIT)
        assertTrue(result is AmountValidationResult.Invalid)
    }

    @Test
    fun `given no method, when validateAmount with valid amount, then returns Valid`() {
        val result = validator.validateAmount(100.0, null)
        assertTrue(result is AmountValidationResult.Valid)
    }
}