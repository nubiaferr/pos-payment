package com.nubiaferr.pospayment.data.remote.service

import com.nubiaferr.pospayment.data.remote.dto.PaymentRequestDto
import com.nubiaferr.pospayment.data.remote.dto.TransactionResponseDto

/**
 * Contract for the acquirer payment service layer.
 *
 * Abstracts the data source so [PaymentRepositoryImpl] does not depend on
 * whether the implementation is a [FakePaymentService] (development/test) or
 * a real [PaymentService] (production).

 */
interface PaymentService {
    suspend fun processPayment(request: PaymentRequestDto): Result<TransactionResponseDto>
    suspend fun cancelTransaction(transactionId: String): Result<TransactionResponseDto>
    suspend fun getTransaction(transactionId: String): Result<TransactionResponseDto>
}
