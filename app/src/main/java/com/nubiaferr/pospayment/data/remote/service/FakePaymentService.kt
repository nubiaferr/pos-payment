package com.nubiaferr.pospayment.data.remote.service

import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import kotlinx.coroutines.delay
import java.util.UUID
import javax.inject.Inject

/**
 * Fake implementation of the payment service layer for development and testing.
 *
 * Simulates realistic network latency and returns synthetic acquirer responses
 * based solely on the business rules already enforced by the domain strategies.
 *
 * Swap this for [PaymentService] in [NetworkModule] once a
 * real acquirer API is available — no other class needs to change.
 *
 * Behaviour:
 * - Always approves payments that reach this layer (domain rules already validated).
 * - Simulates 800ms network latency.
 * - Generates a deterministic auth code from the request amount.
 */
class FakePaymentService @Inject constructor() {

    suspend fun processPayment(request: PaymentRequestDto): Result<TransactionResponseDto> {
        delay(SIMULATED_LATENCY_MS)
        return Result.success(buildResponse(request))
    }

    suspend fun cancelTransaction(transactionId: String): Result<TransactionResponseDto> {
        delay(SIMULATED_LATENCY_MS)
        return Result.success(
            TransactionResponseDto(
                id = transactionId,
                status = "cancelled",
                authCode = "CANCEL-${transactionId.takeLast(4).uppercase()}",
                amount = 0L,
                method = "unknown",
                installments = 1,
                description = "Cancelled",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun getTransaction(transactionId: String): Result<TransactionResponseDto> {
        delay(SIMULATED_LATENCY_MS)
        return Result.success(
            TransactionResponseDto(
                id = transactionId,
                status = "approved",
                authCode = "STATUS-${transactionId.takeLast(4).uppercase()}",
                amount = 0L,
                method = "unknown",
                installments = 1,
                description = "Status check",
                timestamp = System.currentTimeMillis()
            )
        )
    }

    private fun buildResponse(request: PaymentRequestDto) = TransactionResponseDto(
        id = UUID.randomUUID().toString(),
        status = "approved",
        authCode = "AUTH-${(100000..999999).random()}",
        amount = request.amount,
        method = request.method,
        installments = request.installments,
        description = request.description,
        timestamp = System.currentTimeMillis()
    )

    companion object {
        private const val SIMULATED_LATENCY_MS = 800L
    }
}
