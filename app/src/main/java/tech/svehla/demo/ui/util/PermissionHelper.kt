package tech.svehla.demo.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import tech.svehla.demo.R
import timber.log.Timber

object PermissionHelper {

	private val requiredPermissions = listOfNotNull(
		Manifest.permission.ACCESS_FINE_LOCATION,
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else null,
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else null,
	)

	fun allPermissionsGranted(context: Context): Boolean = requiredPermissions.all { permission ->
		isPermissionGranted(context, permission)
	}

	private fun isPermissionGranted(context: Context, permission: String): Boolean {
		return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
	}

	fun requestPermissions(launcher: ActivityResultLauncher<Array<String>>) {
		launcher.launch(requiredPermissions.toTypedArray())
	}

	fun showPermissionsDeniedDialog(activity: Activity) {
		val alertDialogBuilder = AlertDialog.Builder(activity)
		alertDialogBuilder.setMessage(activity.getString(R.string.alert_permissions_denied_message))
			.setCancelable(false)
			.setPositiveButton(activity.getString(R.string.alert_permissions_denied_action)) { _, _ ->
				// open app settings
				val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
				intent.data = Uri.parse("package:${activity.packageName}")
				activity.startActivity(intent)
			}
			.setNegativeButton(activity.getString(R.string.alert_permissions_denied_cancel)) { _, _ ->
				// exit app
				activity.finish()
			}
		val alert = alertDialogBuilder.create()
		alert.show()
	}

	fun verifyGpsEnabled(activity: Activity): Boolean {
		val locationManager: LocationManager? = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
		var gpsEnabled = false

		try {
			gpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
		} catch (exception: Exception) {
			Timber.e(exception, "Error checking if GPS is enabled")
		}

		if (!gpsEnabled) {
			// notify user
			val alertDialogBuilder = AlertDialog.Builder(activity)
			alertDialogBuilder.setMessage(activity.getString(R.string.alert_location_disabled_message))
				.setCancelable(false)
				.setPositiveButton(activity.getString(R.string.alert_location_disabled_action)) { _, _ ->
					val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
					activity.startActivity(intent)
				}
			val alert = alertDialogBuilder.create()
			alert.show()

		}

		return gpsEnabled
	}

	@SuppressLint("MissingPermission")
	fun tryEnableBluetooth(activity: Activity) {
		val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		val adapter = bluetoothManager.adapter
		if (!adapter.isEnabled) {
			if (isPermissionGranted(activity, Manifest.permission.BLUETOOTH_ADMIN)) {
				adapter.enable()
			}
		}
	}
}