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
 *
 * @property service Wraps Retrofit API calls in [Result].
 * @property dao     Room DAO for local transaction persistence.
 * @property mapper  Converts between DTOs and domain entities.
 */
class PaymentRepositoryImpl @Inject constructor(
    private val service: PaymentService,
    private val dao: PaymentDao,
    private val mapper: PaymentDataMapper
) : PaymentRepository {

    override suspend fun processCredit(payment: Payment): Result<Transaction> =
        processAndCache(payment)

    override suspend fun processDebit(payment: Payment): Result<Transaction> =
        processAndCache(payment)

    override suspend fun processPix(payment: Payment): Result<Transaction> =
        processAndCache(payment)

    override suspend fun processVoucher(payment: Payment): Result<Transaction> =
        processAndCache(payment)

    override suspend fun cancelTransaction(transactionId: String): Result<Transaction> {
        val local = dao.getById(transactionId)
        if (local != null && local.status != TransactionStatus.APPROVED.name) {
            return Result.failure(TransactionNotCancellableException(transactionId))
        }

        return service.cancelTransaction(transactionId).fold(
            onSuccess = { dto ->
                val dummyPayment = local?.toDomainPayment() ?: return Result.failure(
                    Exception("Transaction $transactionId not found locally.")
                )
                val transaction = mapper.toDomain(dto, dummyPayment)
                dao.upsert(transaction.toEntity())
                Result.success(transaction)
            },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun getTransactionStatus(transactionId: String): Result<Transaction> {
        val remoteResult = service.getTransaction(transactionId)
        if (remoteResult.isSuccess) {
            val local = dao.getById(transactionId)
            val dummyPayment = local?.toDomainPayment() ?: return Result.failure(
                Exception("Transaction $transactionId not found locally.")
            )
            val transaction = mapper.toDomain(remoteResult.getOrThrow(), dummyPayment)
            dao.upsert(transaction.toEntity())
            return Result.success(transaction)
        }

        val cached = dao.getById(transactionId)
            ?: return Result.failure(Exception("Transaction $transactionId not found."))

        return Result.success(cached.toDomain())
    }

    // --- helpers ---

    private suspend fun processAndCache(payment: Payment): Result<Transaction> {
        val dto = mapper.toRequestDto(payment)
        return service.processPayment(dto).fold(
            onSuccess = { responseDto ->
                val transaction = mapper.toDomain(responseDto, payment)
                dao.upsert(transaction.toEntity())
                Result.success(transaction)
            },
            onFailure = { Result.failure(it) }
        )
    }

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        amount = (payment.amount * 100).toLong(),
        paymentMethod = payment.method.name,
        installments = payment.installments,
        description = payment.description,
        status = status.name,
        authCode = authCode,
        timestamp = timestamp
    )

    private fun TransactionEntity.toDomain(): Transaction = Transaction(
        id = id,
        payment = toDomainPayment(),
        status = TransactionStatus.valueOf(status),
        authCode = authCode,
        timestamp = timestamp
    )

    private fun TransactionEntity.toDomainPayment() = Payment(
        amount = amount / 100.0,
        method = PaymentMethod.valueOf(paymentMethod),
        installments = installments,
        description = description
    )
}