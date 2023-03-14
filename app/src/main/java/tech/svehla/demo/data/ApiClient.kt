package tech.svehla.demo.data

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import tech.svehla.demo.data.model.ServerPaymentIntent
import tech.svehla.demo.api.BackendService
import java.io.IOException

// The 'ApiClient' is a singleton object used to make calls to our backend and return their results
// TODO - convert do class and use with DI
object ApiClient {

    private const val BACKEND_URL = "http://10.0.2.2:4567"

    private val client = OkHttpClient.Builder()
        .addNetworkInterceptor(StethoInterceptor())
        .build()
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BACKEND_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val service: BackendService = retrofit.create(BackendService::class.java)

    @Throws(ConnectionTokenException::class)
    internal fun createConnectionToken(): String {
        try {
            val result = service.getConnectionToken().execute()
            if (result.isSuccessful && result.body() != null) {
                return result.body()!!.secret
            } else {
                throw ConnectionTokenException("Creating connection token failed")
            }
        } catch (e: IOException) {
            throw ConnectionTokenException("Creating connection token failed", e)
        }
    }

    @Throws(Exception::class)
    internal suspend fun createPaymentIntent(
        amount: Int, currency: String, referenceNumber: String
    ): ServerPaymentIntent {
        return service.createPaymentIntent(amount, currency, referenceNumber)
    }

    internal suspend fun capturePaymentIntent(id: String) {
        service.capturePaymentIntent(id)
    }
}