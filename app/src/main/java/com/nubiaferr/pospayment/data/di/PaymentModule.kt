package com.nubiaferr.pospayment.data.di

import com.nubiaferr.pospayment.data.repository.PaymentRepositoryImpl
import com.nubiaferr.pospayment.domain.model.PaymentMethod
import com.nubiaferr.pospayment.domain.repository.PaymentRepository
import com.nubiaferr.pospayment.domain.strategy.CreditPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.DebitPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.PaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.PixPaymentStrategy
import com.nubiaferr.pospayment.domain.strategy.VoucherPaymentStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import javax.inject.Singleton

/**
 * Hilt module that wires the payment domain to its data-layer implementations.
 *
 * Responsibilities:
 * 1. Binds [PaymentRepository] interface → [PaymentRepositoryImpl].
 * 2. Builds the `Map<PaymentMethod, PaymentStrategy>` via `@IntoMap` bindings.
 *
 * Adding a new payment method requires only a new `@Binds @IntoMap` entry here
 * and the corresponding [PaymentStrategy] implementation — no other class changes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    /**
     * Binds the concrete [PaymentRepositoryImpl] to the [PaymentRepository] interface.
     * The domain layer only ever sees the interface.
     */
    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        impl: PaymentRepositoryImpl
    ): PaymentRepository

    /**
     * Registers [CreditPaymentStrategy] in the strategies map under [PaymentMethod.CREDIT].
     */
    @Binds
    @IntoMap
    @PaymentMethodKey(PaymentMethod.CREDIT)
    abstract fun bindCreditStrategy(strategy: CreditPaymentStrategy): PaymentStrategy

    /**
     * Registers [DebitPaymentStrategy] in the strategies map under [PaymentMethod.DEBIT].
     */
    @Binds
    @IntoMap
    @PaymentMethodKey(PaymentMethod.DEBIT)
    abstract fun bindDebitStrategy(strategy: DebitPaymentStrategy): PaymentStrategy

    /**
     * Registers [PixPaymentStrategy] in the strategies map under [PaymentMethod.PIX].
     */
    @Binds
    @IntoMap
    @PaymentMethodKey(PaymentMethod.PIX)
    abstract fun bindPixStrategy(strategy: PixPaymentStrategy): PaymentStrategy

    /**
     * Registers [VoucherPaymentStrategy] in the strategies map under [PaymentMethod.VOUCHER].
     */
    @Binds
    @IntoMap
    @PaymentMethodKey(PaymentMethod.VOUCHER)
    abstract fun bindVoucherStrategy(strategy: VoucherPaymentStrategy): PaymentStrategy
}