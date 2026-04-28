package com.nubiaferr.pospayment.domain.exception

sealed class BusinessException(message: String) : Exception(message)

class UnsupportedPaymentMethodException(
    method: String
) : BusinessException("Payment method '$method' is not supported on this terminal.")

class InstalmentNotAllowedException(
    minAmount: Double
) : BusinessException("Instalment payments require a minimum amount of R$${"%.2f".format(minAmount)}.")

class PixLimitExceededException(
    limit: Double
) : BusinessException("Pix transactions are limited to R$${"%.2f".format(limit)} per transaction.")

class DebitLimitExceededException(
    limit: Double
) : BusinessException("Débito não permite transações acima de R$${"%.2f".format(limit)}.")

class VoucherLimitExceededException(
    limit: Double
) : BusinessException("Voucher não permite transações acima de R$${"%.2f".format(limit)}.")

class TransactionNotCancellableException(
    transactionId: String
) : BusinessException("Transaction '$transactionId' cannot be cancelled in its current state.")