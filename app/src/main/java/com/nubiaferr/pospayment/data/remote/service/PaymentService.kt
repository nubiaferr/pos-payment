package com.nubiaferr.pospayment.data.remote.service

import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto

/**
 * Contract for the acquirer payment service layer.
 *
 * Abstracts the data source so [com.nubiaferr.pospayment.data.repository.PaymentRepositoryImpl] does not depend on
 * whether the implementation is [FakePaymentService] (development) or a
 * real Retrofit-backed service (production).
 */
interface PaymentService {
    suspend fun processPayment(request: PaymentRequestDto): Result<TransactionResponseDto>
    suspend fun cancelTransaction(transactionId: String): Result<TransactionResponseDto>
    suspend fun getTransaction(transactionId: String): Result<TransactionResponseDto>
}