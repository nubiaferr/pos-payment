package com.nubiaferr.pospayment.domain.validation

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
        // Previously the validator would clamp 600 → 12 silently.
        // Now it must return Invalid so the operator sees an explicit error.
        assertTrue(validator.validateInstallments("600") is InstalmentsValidationResult.Invalid)
    }
}