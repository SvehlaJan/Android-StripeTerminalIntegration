package tech.svehla.demo.data

import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.Reader
import timber.log.Timber

class TerminalEventListener : TerminalListener {
    override fun onUnexpectedReaderDisconnect(reader: Reader) {
        Timber.d("Reader disconnected unexpectedly: ${reader.serialNumber}")
        // Show UI that your reader disconnected
    }
}