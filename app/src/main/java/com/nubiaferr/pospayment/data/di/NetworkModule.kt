package com.nubiaferr.pospayment.data.di

import android.content.Context
import androidx.room.Room
import com.nubiaferr.pospayment.data.local.PaymentDatabase
import com.nubiaferr.pospayment.data.local.dao.PaymentDao
import com.nubiaferr.pospayment.data.remote.service.PaymentApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module that provides network and database infrastructure dependencies.
 *
 * All bindings are [Singleton] — a single Retrofit instance and a single Room
 * database are shared across the entire app lifetime.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://api.btgpactual.com/pos/"
    private const val TIMEOUT_SECONDS = 30L
    private const val DB_NAME = "pos_payment.db"

    /**
     * Provides a configured [OkHttpClient] with logging and timeouts appropriate
     * for a POS terminal environment (longer timeouts for unstable connections).
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    /**
     * Provides the [Retrofit] instance configured with the acquirer base URL.
     *
     * @param client The [OkHttpClient] to use for all requests.
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    /**
     * Provides the [PaymentApi] Retrofit interface implementation.
     *
     * @param retrofit The configured [Retrofit] instance.
     */
    @Provides
    @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi =
        retrofit.create(PaymentApi::class.java)

    /**
     * Provides the Room [PaymentDatabase] instance.
     *
     * @param context Application context required by Room.
     */
    @Provides
    @Singleton
    fun providePaymentDatabase(
        @ApplicationContext context: Context
    ): PaymentDatabase = Room.databaseBuilder(
        context,
        PaymentDatabase::class.java,
        DB_NAME
    ).build()

    /**
     * Provides the [PaymentDao] extracted from the database instance.
     *
     * @param database The [PaymentDatabase] instance.
     */
    @Provides
    @Singleton
    fun providePaymentDao(database: PaymentDatabase): PaymentDao =
        database.paymentDao()
}