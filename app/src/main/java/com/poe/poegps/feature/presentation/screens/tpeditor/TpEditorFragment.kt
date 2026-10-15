package com.poe.poegps.feature.presentation.screens.tpeditor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.permissionx.guolindev.PermissionX
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentTpEditorBinding
import com.poe.poegps.feature.data.remote.utils.MyLocationListener
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.dialog.LocationToggleDialogFragment
import com.poe.poegps.feature.presentation.screens.tpeditor.dialog.OnTpLongClickDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow
import java.util.*

@AndroidEntryPoint
class TpEditorFragment : Fragment(), View.OnClickListener, MyLocationListener {

    private val args: TpEditorFragmentArgs by lazy {
        TpEditorFragmentArgs.fromBundle(requireArguments())
    }

    private lateinit var tpToSave: OprDisplayable

    private val viewModel by viewModels<TpEditorViewModel>()

    private var binding : FragmentTpEditorBinding by autoCleaned()

    private var locationManager: LocationManager? = null

    override fun onStart() {
        super.onStart()
        startLocationUpdates()
    }

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tpName.text = args.pltxt

        collectLifecycleFlow(viewModel.tpObject) { tp ->
            binding.opr.text = tp.name
            binding.lat.text = tp.lat
            binding.lng.text = tp.lng
            if (tp.lat != "0.0" && tp.lng != "0.0") {
                binding.takeCoord.isEnabled = false
            }
            tpToSave = tp.copy()
        }

        binding.textViews.setOnLongClickListener {
            OnTpLongClickDialogFragment.show(childFragmentManager, args.tplnr)
            true
        }

        binding.takeCoord.setOnClickListener {
            if (binding.include.GPSLatitude.text != "0.0" && locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                //viewModel.saveCoordinates(opr)
                binding.lat.text = binding.include.GPSLatitude.text.toString()
                binding.lng.text  = binding.include.GPSLongitude.text.toString()
                binding.takeCoord.isEnabled = false
                viewModel.saveTpCoord(tpToSave.copy(lat = binding.lat.text.toString(), lng = binding.lng.text.toString()))
            } else if (binding.include.GPSLatitude.text == "0.0" && locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                Toast.makeText(
                    requireContext(),
                    "Заждіть доки не знайдуться нові спутники!",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Будь-ласка, ввімкніть GPS!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.backBtn.setOnClickListener(this)

        viewModel.fetchTpObject(args.tplnr, args.isAbon)
        setupOnTpLongClickListener()
    }

    private fun setupOnTpLongClickListener() {
        OnTpLongClickDialogFragment.setupListeners(childFragmentManager, viewLifecycleOwner) {
                _, which ->
            when (which) {
                getString(R.string.clear_tp_coord) -> {
                    binding.lat.text = "0.0"
                    binding.lng.text = "0.0"
                    binding.takeCoord.isEnabled = true
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> {
                findNavController().popBackStack()
            }
        }
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
                    startLocationUpdates()
                    if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) {
                        LocationToggleDialogFragment.show(parentFragmentManager)
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

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationManager == null && hasLocationPermission()) {
            locationManager =
                requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 2000L,
                0f, this
            )
        }
        checkEnabled()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onProviderEnabled(provider: String) {
        checkEnabled()
    }

    override fun onProviderDisabled(provider: String) {
        checkEnabled()
    }

    private fun checkEnabled() {
        if (view == null) return
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