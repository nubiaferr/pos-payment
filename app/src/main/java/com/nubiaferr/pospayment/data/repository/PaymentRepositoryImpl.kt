package com.nubiaferr.pospayment.data.repository

import com.nubiaferr.pospayment.data.local.dao.PaymentDao
import com.nubiaferr.pospayment.data.local.entity.TransactionEntity
import com.nubiaferr.pospayment.data.mapper.PaymentDataMapper
import com.nubiaferr.pospayment.data.remote.service.PaymentService
import com.nubiaferr.pospayment.domain.exception.TransactionNotCancellableException
import com.nubiaferr.pospayment.domain.model.Payment
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.model.Transaction
import com.nubiaferr.pospayment.domain.model.TransactionStatus
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import javax.inject.Inject

/**
 * Concrete implementation of [PaymentRepository].
 *
 * Orchestrates between remote ([PaymentService]) and local ([PaymentDao]) data sources:
 * - Every successful remote transaction is persisted locally for offline access.
 * - [getTransactionStatus] falls back to the local cache when the network is unavailable.
 * - [cancelTransaction] validates the current status before delegating to the API.
 */
class PaymentRepositoryImpl @Inject constructor(
    private val service: PaymentService,
    private val dao: PaymentDao,
    private val mapper: PaymentDataMapper
) : PaymentRepository {

    override suspend fun processPayment(payment: Payment): Result<Transaction> {
        val dto = mapper.toRequestDto(payment)
        return service.processPayment(dto).fold(
            onSuccess = { responseDto ->
                val transaction = mapper.toDomain(responseDto, payment)
                dao.upsert(mapper.toEntity(transaction))
                Result.success(transaction)
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun cancelTransaction(transactionId: String): Result<Transaction> {
        val local = dao.getById(transactionId)

        if (local != null && local.status != TransactionStatus.APPROVED.name) {
            return Result.failure(TransactionNotCancellableException(transactionId))
        }

        return service.cancelTransaction(transactionId).fold(
            onSuccess = { dto ->
                val originalPayment = local?.toDomainPayment()
                    ?: return Result.failure(
                        Exception("Transaction $transactionId not found locally.")
                    )
                val transaction = mapper.toDomain(dto, originalPayment)
                dao.upsert(mapper.toEntity(transaction))
                Result.success(transaction)
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun getTransactionStatus(transactionId: String): Result<Transaction> {
        val remoteResult = service.getTransaction(transactionId)

        if (remoteResult.isSuccess) {
            val local = dao.getById(transactionId)
            val originalPayment = local?.toDomainPayment()
                ?: return Result.failure(
                    Exception("Transaction $transactionId not found locally.")
                )
            val transaction = mapper.toDomain(remoteResult.getOrThrow(), originalPayment)
            dao.upsert(mapper.toEntity(transaction))
            return Result.success(transaction)
        }

        val cached = dao.getById(transactionId)
            ?: return Result.failure(Exception("Transaction $transactionId not found."))

        return Result.success(cached.toDomain())
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun TransactionEntity.toDomainPayment() = Payment(
        amount = amount / 100.0,
        method = PaymentMethod.valueOf(paymentMethod),
        installments = installments,
        description = description
    )

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        payment = toDomainPayment(),
        status = TransactionStatus.valueOf(status),
        authCode = authCode,
        timestamp = timestamp
    )
}