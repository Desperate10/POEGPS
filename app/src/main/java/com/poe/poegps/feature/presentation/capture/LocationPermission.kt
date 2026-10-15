package com.poe.poegps.feature.presentation.capture

import android.Manifest
import androidx.fragment.app.Fragment
import com.permissionx.guolindev.PermissionX
import com.poe.poegps.R

/**
 * Asks for location permission and, once granted, calls [onGranted]. Whether GPS is on is the
 * checklist's job (B12, [ReadyChecklist]); a refusal keeps the checklist up.
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
            if (allGranted) onGranted()
        }
}
