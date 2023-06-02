package com.poe.poegps.feature.presentation.screens.tpeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.permissionx.guolindev.PermissionX
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentTpEditorBinding
import com.poe.poegps.feature.data.remote.utils.MyLocationListener
import com.poe.poegps.feature.presentation.screens.editor.EditorFragment
import com.poe.poegps.feature.presentation.screens.editor.EditorFragmentArgs
import com.poe.poegps.feature.presentation.screens.editor.dialog.LocationToggleDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned
import java.util.*

@AndroidEntryPoint
class TpEditorFragment : Fragment(), View.OnClickListener, MyLocationListener {

    private val args: TpEditorFragmentArgs by lazy {
        TpEditorFragmentArgs.fromBundle(requireArguments())
    }

    private var binding : FragmentTpEditorBinding by autoCleaned()

    private var locationManager: LocationManager? = null

    override fun onStop() {
        super.onStop()
        if (locationManager != null) {
            locationManager?.removeUpdates(this)
            locationManager = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkEnabled()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTpEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> {
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onLocationChanged(location: Location) {
        binding.include.GPSLatitude.text = location.latitude.toString()
        binding.include.GPSLongitude.text = location.longitude.toString()
        if (location.provider.equals(LocationManager.GPS_PROVIDER)) {
            binding.include.GPSLatitude.text = formatLocationLat(location)
            binding.include.GPSLongitude.text = formatLocationLng(location)
            if (location.hasAccuracy()) {
                binding.include.GPSAccuracy.text = String.format("%4.1f", location.accuracy)
            } else {
                binding.include.GPSAccuracy.text = "000.0"
            }
        }
    }

    private fun formatLocationLat(location: Location?): String {
        return if (location == null) "" else String.format(
            Locale.US,
            "%1$.5f",
            location.latitude
        )
    }


    private fun formatLocationLng(location: Location?): String {
        return if (location == null) "" else String.format(
            Locale.US,
            "%1$.5f",
            location.longitude
        )
    }

    @SuppressLint("MissingPermission")
    private fun checkPermissions() {
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
            .request { allGranted, _, deniedList ->
                if (allGranted) {
                    locationManager =
                        requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    locationManager?.let {
                        it.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, EditorFragment.EVERY_SECOND,
                            EditorFragment.EVERY_0M, this
                        )
                        if (!it.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            LocationToggleDialogFragment.show(parentFragmentManager)
                        }
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "${getString(R.string.denied_permissions_text)} $deniedList",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun checkEnabled() {
        if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            binding.include.GPSStatus.text = "ON"
        } else {
            binding.include.GPSStatus.text = "OFF"
            binding.include.GPSLatitude.text = "0.0"
            binding.include.GPSLongitude.text = "0.0"
            binding.include.GPSAccuracy.text = "000.0"
        }
    }


}