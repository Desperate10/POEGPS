package com.poe.poegps.feature.presentation.capture

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.fragment.app.Fragment
import com.permissionx.guolindev.PermissionX
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.screens.editor.dialog.LocationToggleDialogFragment

/**
 * Asks for location permission and, once granted, calls [onGranted] and offers to switch GPS on.
 * A refusal needs no message here: the GPS chip and the capture button say so and ask again.
 */
fun Fragment.requestLocation(onGranted: () -> Unit) {
    PermissionX.init(this)
        .permissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        .onExplainRequestReason { scope, deniedList ->
            scope.showRequestReasonDialog(
                deniedList,
                getString(R.string.explain_permission_text),
                getString(R.string.yes), getString(R.string.cancel)
            )
        }
        .onForwardToSettings { scope, deniedList ->
            scope.showForwardToSettingsDialog(
                deniedList,
                getString(R.string.forward_to_settings_text),
                getString(R.string.yes), getString(R.string.cancel)
            )
        }
        .request { allGranted, _, _ ->
            if (allGranted) {
                onGranted()
                if (!isGpsEnabled()) LocationToggleDialogFragment.show(parentFragmentManager)
            }
        }
}

/** The GPS chip's "fix it" action: ask for permission, or open the GPS switch. */
fun Fragment.fixGps(hasPermission: Boolean, onGranted: () -> Unit) {
    if (!hasPermission) requestLocation(onGranted)
    else LocationToggleDialogFragment.show(parentFragmentManager)
}

private fun Fragment.isGpsEnabled(): Boolean =
    (requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager)
        .isProviderEnabled(LocationManager.GPS_PROVIDER)
