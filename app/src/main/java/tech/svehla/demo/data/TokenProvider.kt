package tech.svehla.demo.data

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import timber.log.Timber

// A simple implementation of the [ConnectionTokenProvider] interface. We just request a
// new token from our backend simulator and forward any exceptions along to the SDK.
class TokenProvider : ConnectionTokenProvider {

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        try {
            val token = ApiClient.createConnectionToken()
            callback.onSuccess(token)
        } catch (e: ConnectionTokenException) {
            Timber.e(e, "Failed to fetch connection token")
            callback.onFailure(e)
        }
    }
}