package com.nubiaferr.pospayment.data.remote.service

import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for the acquirer payment API.
 *
 * All functions are `suspend` for use with Kotlin Coroutines.
 * Responses are wrapped in [Response] so the service layer can inspect HTTP codes.
 */
interface PaymentApi {

    /**
     * Initiates a new payment transaction with the acquirer.
     *
     * @param request The payment request body.
     * @return [Response] wrapping the [TransactionResponseDto] on success.
     */
    @POST("v1/transactions")
    suspend fun processPayment(
        @Body request: PaymentRequestDto
    ): Response<TransactionResponseDto>

    /**
     * Cancels a previously approved transaction.
     *
     * @param transactionId The unique identifier of the transaction to cancel.
     * @return [Response] wrapping the updated [TransactionResponseDto].
     */
    @POST("v1/transactions/{id}/cancel")
    suspend fun cancelTransaction(
        @Path("id") transactionId: String
    ): Response<TransactionResponseDto>

    /**
     * Retrieves the current status of a transaction.
     *
     * @param transactionId The unique identifier of the transaction.
     * @return [Response] wrapping the [TransactionResponseDto].
     */
    @GET("v1/transactions/{id}")
    suspend fun getTransaction(
        @Path("id") transactionId: String
    ): Response<TransactionResponseDto>
}