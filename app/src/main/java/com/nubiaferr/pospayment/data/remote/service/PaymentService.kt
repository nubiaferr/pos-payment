package com.nubiaferr.pospayment.data.remote.service

import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import javax.inject.Inject

/**
 * Thin service layer that wraps [PaymentApi] calls in [Result].
 *
 * Responsibilities:
 * - Converts HTTP error responses into [Exception] failures.
 * - Catches network-level exceptions (timeout, no connectivity).
 * - Provides a clean `Result<T>` contract to [PaymentRepositoryImpl].
 *
 * @property api The Retrofit interface for the acquirer API.
 */
class PaymentService @Inject constructor(
    private val api: PaymentApi
) {

    /**
     * Sends a payment request to the acquirer.
     *
     * @param request The payment request DTO.
     * @return [Result.success] with [TransactionResponseDto] on HTTP 200-299,
     *         [Result.failure] with an [HttpException] for other HTTP codes,
     *         or [Result.failure] with an [IOException] for network errors.
     */
    suspend fun processPayment(request: PaymentRequestDto): Result<TransactionResponseDto> =
        safeApiCall { api.processPayment(request) }

    /**
     * Sends a cancellation request for a given transaction.
     *
     * @param transactionId The transaction to cancel.
     * @return [Result.success] with the updated [TransactionResponseDto], or [Result.failure].
     */
    suspend fun cancelTransaction(transactionId: String): Result<TransactionResponseDto> =
        safeApiCall { api.cancelTransaction(transactionId) }

    /**
     * Fetches the current status of a transaction from the acquirer.
     *
     * @param transactionId The transaction to look up.
     * @return [Result.success] with [TransactionResponseDto], or [Result.failure].
     */
    suspend fun getTransaction(transactionId: String): Result<TransactionResponseDto> =
        safeApiCall { api.getTransaction(transactionId) }

    /**
     * Wraps a Retrofit [Response] call in [Result], converting HTTP errors and
     * network exceptions into [Result.failure].
     */
    private suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) Result.success(body)
                else Result.failure(Exception("Response body is null (HTTP ${response.code()})"))
            } else {
                Result.failure(Exception("HTTP error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}