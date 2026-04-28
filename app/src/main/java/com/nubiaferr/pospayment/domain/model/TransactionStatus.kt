package com.nubiaferr.pospayment.domain.model

/**
 * Lifecycle states a [Transaction] can be in after processing.
 */
enum class TransactionStatus {
    /** Authorized by the acquirer and settled. */
    APPROVED,

    /** Declined by the acquirer or card network. */
    DECLINED,

    /** Successfully reversed after a prior approval. */
    CANCELLED,

    /** Terminal sent the request but no response was received (e.g. network timeout). */
    PENDING
}
