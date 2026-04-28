package com.nubiaferr.pospayment.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
* Request body sent to the acquirer API to initiate a payment.
*
* This is a data-layer concern — if the API contract changes, only this class
* and [com.nubiaferr.pospayment.data.mapper.PaymentDataMapper] need to be updated. The domain layer is unaffected.
*
* @property amount Transaction amount in BRL cents (e.g. 1000 = R$10.00).
* @property method Payment method identifier as expected by the API.
* @property installments Number of instalments. Defaults to 1.
* @property description Optional merchant description.
*/
data class PaymentRequestDto(
    @SerializedName("amount") val amount: Long,
    @SerializedName("payment_method") val method: String,
    @SerializedName("installments") val installments: Int,
    @SerializedName("description") val description: String
)

/**
 * Response body received from the acquirer API after a payment attempt.
 *
 * @property id Unique transaction identifier assigned by the acquirer.
 * @property status Transaction status string (e.g. "approved", "declined").
 * @property authCode Authorization code returned by the card network.
 * @property amount Confirmed amount in BRL cents.
 * @property method Payment method used.
 * @property installments Number of instalments confirmed.
 * @property timestamp ISO-8601 timestamp of the transaction.
 */
data class TransactionResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("status") val status: String,
    @SerializedName("auth_code") val authCode: String,
    @SerializedName("amount") val amount: Long,
    @SerializedName("payment_method") val method: String,
    @SerializedName("installments") val installments: Int,
    @SerializedName("description") val description: String,
    @SerializedName("created_at") val timestamp: Long
)
