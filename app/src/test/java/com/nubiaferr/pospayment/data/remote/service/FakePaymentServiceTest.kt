package com.nubiaferr.pospayment.data.remote.service

import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [FakePaymentService].
 *
 * Verifies that the fake always returns consistent, well-formed responses
 * so tests that depend on it have a stable contract to assert against.
 */
class FakePaymentServiceTest {

    private lateinit var service: FakePaymentService

    @Before
    fun setUp() {
        service = FakePaymentService()
    }

    // ── processPayment ─────────────────────────────────────────────────────────

    @Test
    fun `when processPayment called, then always returns success`() = runTest {
        val request = PaymentRequestDto(
            amount = 10_000L,
            method = "credit",
            installments = 1,
            description = "Test"
        )

        val result = service.processPayment(request)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `when processPayment called, then response has approved status`() = runTest {
        val request = PaymentRequestDto(10_000L, "credit", 1, "Test")

        val response = service.processPayment(request).getOrThrow()

        assertEquals("approved", response.status)
    }

    @Test
    fun `when processPayment called, then response mirrors request amount and method`() = runTest {
        val request = PaymentRequestDto(25_000L, "pix", 1, "Lunch")

        val response = service.processPayment(request).getOrThrow()

        assertEquals(25_000L, response.amount)
        assertEquals("pix", response.method)
    }

    @Test
    fun `when processPayment called, then response has non-blank auth code`() = runTest {
        val request = PaymentRequestDto(5_000L, "debit", 1, "")

        val response = service.processPayment(request).getOrThrow()

        assertTrue(response.authCode.isNotBlank())
    }

    @Test
    fun `when processPayment called, then each response has a unique id`() = runTest {
        val request = PaymentRequestDto(5_000L, "credit", 1, "")

        val id1 = service.processPayment(request).getOrThrow().id
        val id2 = service.processPayment(request).getOrThrow().id

        assertTrue(id1 != id2)
    }

    // ── cancelTransaction ──────────────────────────────────────────────────────

    @Test
    fun `when cancelTransaction called, then always returns success`() = runTest {
        val result = service.cancelTransaction("txn_abc123")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `when cancelTransaction called, then response has cancelled status`() = runTest {
        val response = service.cancelTransaction("txn_abc123").getOrThrow()

        assertEquals("cancelled", response.status)
    }

    @Test
    fun `when cancelTransaction called, then response id matches requested transaction id`() = runTest {
        val response = service.cancelTransaction("txn_abc123").getOrThrow()

        assertEquals("txn_abc123", response.id)
    }

    // ── getTransaction ─────────────────────────────────────────────────────────

    @Test
    fun `when getTransaction called, then always returns success`() = runTest {
        val result = service.getTransaction("txn_xyz789")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `when getTransaction called, then response has approved status`() = runTest {
        val response = service.getTransaction("txn_xyz789").getOrThrow()

        assertEquals("approved", response.status)
    }

    @Test
    fun `when getTransaction called, then response id matches requested transaction id`() = runTest {
        val response = service.getTransaction("txn_xyz789").getOrThrow()

        assertEquals("txn_xyz789", response.id)
    }

    @Test
    fun `when getTransaction called, then response has non-null timestamp`() = runTest {
        val response = service.getTransaction("txn_xyz789").getOrThrow()

        assertNotNull(response.timestamp)
        assertTrue(response.timestamp > 0)
    }
}
