package com.nubiaferr.pospayment.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nubiaferr.pospayment.data.local.entity.TransactionEntity

/**
* Data Access Object for local transaction persistence.
*
* All functions are `suspend` for safe use within coroutines.
*/
@Dao
interface PaymentDao {

    /**
     * Inserts or updates a transaction in the local database.
     *
     * Uses [OnConflictStrategy.REPLACE] so that status updates (e.g. PENDING -> APPROVED)
     * are reflected correctly.
     *
     * @param transaction The [TransactionEntity] to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transaction: TransactionEntity)

    /**
     * Retrieves a transaction by its unique identifier.
     *
     * @param id The transaction ID to look up.
     * @return The [TransactionEntity] if found, or `null` if not cached locally.
     */
    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    /**
     * Returns all locally stored transactions, ordered by most recent first.
     *
     * @return List of all [TransactionEntity] records.
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAll(): List<TransactionEntity>
}