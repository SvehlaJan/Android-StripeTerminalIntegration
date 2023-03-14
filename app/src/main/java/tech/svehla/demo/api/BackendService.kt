package tech.svehla.demo.api

import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import tech.svehla.demo.data.model.ConnectionToken
import tech.svehla.demo.data.model.ServerPaymentIntent

/**
 * The 'BackendService' interface handles the two simple calls we need to make to our backend.
 * This represents YOUR backend, so feel free to change the routes accordingly.
 */
interface BackendService {

    /**
     * Get a connection token string from the backend
     */
    @POST("connection_token")
    fun getConnectionToken(): Call<ConnectionToken>

    /**
     * Create a payment intent on the backend
     */
    @FormUrlEncoded
    @POST("create_payment_intent")
    suspend fun createPaymentIntent(
        @Field("amount") amount: Int,
        @Field("currency") currency: String,
        @Field("referenceNumber") referenceNumber: String,
    ): ServerPaymentIntent

    /**
     * Capture a specific payment intent on our backend
     */
    @FormUrlEncoded
    @POST("capture_payment_intent")
    suspend fun capturePaymentIntent(@Field("payment_intent_id") id: String): Any?
}