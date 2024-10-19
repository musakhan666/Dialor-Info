package com.pentuss.dialerinfo.util

import android.app.Activity
import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.ViewModel

private const val REQUEST_CODE_PERMISSIONS = 123 // Arbitrary request code

class PermissionsViewModel : ViewModel() {

    // Replaced permissions list with the ones required for telephony
    val requiredPermissions = mutableListOf(
        "android.permission.READ_PHONE_STATE",       // Required to read phone state (incoming/outgoing calls)
        "android.permission.READ_CALL_LOG",          // Required to access call logs
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.POST_NOTIFICATIONS"
    // Required to detect outgoing calls
    )

    fun checkPermissions(context: Context, callback: (Boolean) -> Unit) {
        val missingPermissions = requiredPermissions.filter {
            val permissionGranted = ContextCompat.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
            Log.d("PermissionsCheck", "Permission: $it, Granted: $permissionGranted")
            !permissionGranted
        }



        if (missingPermissions.isNotEmpty()) {
            // Show the dialog
            callback.invoke(true) // Permissions are missing
        } else {
            // All permissions are granted
            callback.invoke(false)
        }
    }

    fun requestPermissions(context: Context) {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PermissionChecker.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            val activity = context as Activity
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }
}
