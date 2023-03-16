package tech.svehla.demo

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.svehla.demo.presentation.MainScreen
import tech.svehla.demo.presentation.MainVM
import tech.svehla.demo.presentation.MainVmContract
import tech.svehla.demo.ui.theme.DemoTheme
import timber.log.Timber

class MainActivity : ComponentActivity() {

    private val mainVM: MainVM by lazy {
        ViewModelProvider(this).get(MainVM::class.java)
    }

    private var permissionsAlreadyDenied = false

    /**
     * Upon starting, we should verify we have the permissions we need, then start the app
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BluetoothAdapter.getDefaultAdapter()?.isEnabled == false) {
            BluetoothAdapter.getDefaultAdapter().enable()
        }

        setContent {
            DemoTheme() {
                Scaffold { innerPadding ->
                    val uiState by mainVM.uiState.collectAsStateWithLifecycle()
                    uiState.events.firstOrNull()?.let { event ->
                        LaunchedEffect(event) {
                            when (event) {
                                is MainVmContract.UiEvent.NavigateBack -> {
                                    onBackPressed()
                                }
                            }
                            // Once the event is consumed, notify the ViewModel.
                            mainVM.onEventConsumed(event)
                        }
                    }

                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onUiAction = { mainVM.onUiAction(it) },
                    )
                }
            }
        }
    }

    // Register the permissions callback to handles the response to the system permissions dialog.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onPermissionResult(it)
    }

    override fun onResume() {
        super.onResume()

        requestPermissionsIfNecessary()
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissionsIfNecessary() {
        if (Build.VERSION.SDK_INT >= 31) {
            val deniedPermissions = mutableListOf<String>().apply {
                if (!isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) add(Manifest.permission.ACCESS_FINE_LOCATION)
                if (!isGranted(Manifest.permission.BLUETOOTH_CONNECT)) add(Manifest.permission.BLUETOOTH_CONNECT)
                if (!isGranted(Manifest.permission.BLUETOOTH_SCAN)) add(Manifest.permission.BLUETOOTH_SCAN)
            }.toTypedArray()

            if (deniedPermissions.isNotEmpty()) {
                // If we don't have them yet, request them before doing anything else
                requestPermissions(deniedPermissions)
            } else if (!mainVM.isStripeSDKInitialized() && verifyGpsEnabled()) {
                mainVM.initializeStripeSDK(applicationContext = applicationContext)
            }
        } else {
            if (!isGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                // If we don't have them yet, request them before doing anything else
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
            } else if (!mainVM.isStripeSDKInitialized() && verifyGpsEnabled()) {
                mainVM.initializeStripeSDK(applicationContext = applicationContext)
            }
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        if (!permissionsAlreadyDenied) {
            requestPermissionLauncher.launch(permissions)
        } else {
            Timber.d("Permissions already denied. Not requesting again.")
            // If we don't have the permissions we need, notify the user and exit
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setMessage(getString(R.string.alert_permissions_denied_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.alert_permissions_denied_action)) { _, _ ->
                    // open app settings
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
                .setNegativeButton(getString(R.string.alert_permissions_denied_cancel)) { _, _ ->
                    // exit app
                    finish()
                }
            val alert = alertDialogBuilder.create()
            alert.show()
        }
    }

    /**
     * Receive the result of our permissions check, and initialize if we can
     */
    private fun onPermissionResult(result: Map<String, Boolean>) {
        val deniedPermissions: List<String> = result
            .filter { !it.value }
            .map { it.key }

        // If we receive a response to our permission check, initialize
        if (deniedPermissions.isEmpty() && !mainVM.isStripeSDKInitialized() && verifyGpsEnabled()) {
            mainVM.initializeStripeSDK(applicationContext = applicationContext)
        } else {
            permissionsAlreadyDenied = true
        }
    }

    private fun verifyGpsEnabled(): Boolean {
        val locationManager: LocationManager? =
            applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        var gpsEnabled = false

        try {
            gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        } catch (exception: Exception) {
            Timber.e(exception, "Error checking if GPS is enabled")
        }

        if (!gpsEnabled) {
            // notify user
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setMessage(getString(R.string.alert_location_disabled_message))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.alert_location_disabled_action)) { _, _ ->
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }
            val alert = alertDialogBuilder.create()
            alert.show()

        }

        return gpsEnabled
    }
}