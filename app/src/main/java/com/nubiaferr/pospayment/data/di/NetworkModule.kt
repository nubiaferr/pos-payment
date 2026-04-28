package com.nubiaferr.pospayment.data.di

import android.content.Context
import androidx.room.Room
import com.nubiaferr.pospayment.BuildConfig
import com.nubiaferr.pospayment.data.local.PaymentDatabase
import com.nubiaferr.pospayment.data.local.dao.PaymentDao
import com.nubiaferr.pospayment.data.remote.service.FakePaymentService
import com.nubiaferr.pospayment.data.remote.service.PaymentService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides network and database infrastructure dependencies.
 *
 * All bindings are [Singleton] — one Retrofit instance and one Room database
 * shared across the entire app lifetime.
 *
 * SWAP NOTE: To go live with a real acquirer API:
 * 1. Uncomment [providePaymentApi] and add Retrofit + PaymentApi imports.
 * 2. Replace [providePaymentService] to wrap [PaymentApi] in a real [PaymentService].
 * No other class needs to change — [PaymentRepositoryImpl] depends only on the interface.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.btgpactual.com/pos/"
    private const val TIMEOUT_SECONDS = 30L
    private const val DB_NAME = "pos_payment.db"

    /**
     * Logging is enabled only in debug builds to prevent sensitive card/PII data
     * from appearing in logcat in production POS environments.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
            .build()
    }

    /**
     * Provides the active [PaymentService] implementation.
     * Currently [FakePaymentService] — swap for a real implementation when going live.
     */
    @Provides
    @Singleton
    fun providePaymentService(fake: FakePaymentService): PaymentService = fake

    @Provides
    @Singleton
    fun providePaymentDatabase(
        @ApplicationContext context: Context
    ): PaymentDatabase = Room.databaseBuilder(
        context,
        PaymentDatabase::class.java,
        DB_NAME
    ).build()

    @Provides
    @Singleton
    fun providePaymentDao(database: PaymentDatabase): PaymentDao =
        database.paymentDao()

    @Provides
    @Singleton
    fun provideCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.IO
}