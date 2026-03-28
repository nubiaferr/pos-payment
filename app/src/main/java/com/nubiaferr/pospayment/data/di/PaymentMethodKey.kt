package com.nubiaferr.pospayment.data.di

import com.nubiaferr.pospayment.domain.model.PaymentMethod
import dagger.MapKey

/**
 * Custom Hilt/Dagger map key that uses [PaymentMethod] as the key type.
 *
 * Used in conjunction with `@IntoMap` to build the
 * `Map<PaymentMethod, PaymentStrategy>` injected into
 * [ProcessPaymentUseCase].
 *
 * Each strategy binding declares `@PaymentMethodKey(PaymentMethod.CREDIT)` etc.
 */
@MapKey
annotation class PaymentMethodKey(val value: PaymentMethod)