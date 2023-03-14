package tech.svehla.demo

import android.app.Application
import com.stripe.stripeterminal.TerminalApplicationDelegate
import timber.log.Timber

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        TerminalApplicationDelegate.onCreate(this)
    }
}