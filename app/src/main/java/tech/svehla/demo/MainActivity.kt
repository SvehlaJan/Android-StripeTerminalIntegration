package tech.svehla.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.svehla.demo.presentation.MainScreen
import tech.svehla.demo.presentation.MainVM
import tech.svehla.demo.presentation.MainVmContract
import tech.svehla.demo.ui.theme.DemoTheme
import tech.svehla.demo.ui.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val mainVM: MainVM by lazy {
        ViewModelProvider(this).get(MainVM::class.java)
    }

    private var permissionsAlreadyDenied = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (PermissionHelper.allPermissionsGranted(this) && PermissionHelper.verifyGpsEnabled(this)) {
            mainVM.initializeStripeSDK(applicationContext = applicationContext)
        } else {
            permissionsAlreadyDenied = true
        }
    }

    override fun onResume() {
        super.onResume()

        if (PermissionHelper.allPermissionsGranted(this) && PermissionHelper.verifyGpsEnabled(this)) {
            PermissionHelper.tryEnableBluetooth(this)
            mainVM.initializeStripeSDK(applicationContext = applicationContext)
        } else if (permissionsAlreadyDenied) {
            PermissionHelper.showPermissionsDeniedDialog(this)
        } else {
            PermissionHelper.requestPermissions(requestPermissionLauncher)
        }
    }
}