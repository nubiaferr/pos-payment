package com.nubiaferr.pospayment.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity that persists a [Transaction] locally.
 *
 * Used to:
 * - Cache approved transactions for offline receipt display.
 * - Store pending transactions for retry after connectivity is restored.
 *
 * @property id Unique transaction identifier (from the acquirer).
 * @property amount Transaction amount in BRL cents.
 * @property paymentMethod Payment method used (stored as string for Room compatibility).
 * @property installments Number of instalments.
 * @property description Merchant description.
 * @property status Transaction status string.
 * @property authCode Authorization code from the acquirer.
 * @property timestamp Unix epoch (ms) of the transaction.
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val paymentMethod: String,
    val installments: Int,
    val description: String,
    val status: String,
    val authCode: String,
    val timestamp: Long
)