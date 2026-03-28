package com.nubiaferr.pospayment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nubiaferr.pospayment.data.local.dao.PaymentDao
import com.nubiaferr.pospayment.data.local.entity.TransactionEntity

/**
 * Room database for the POS payment module.
 *
 * Increment [version] and provide a [androidx.room.migration.Migration] whenever
 * the schema changes — never use `fallbackToDestructiveMigration` in production
 * POS environments, as locally cached transactions must be preserved.
 */
@Database(
    entities = [TransactionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class PaymentDatabase : RoomDatabase() {

    /** Provides access to transaction persistence operations. */
    abstract fun paymentDao(): PaymentDao
}