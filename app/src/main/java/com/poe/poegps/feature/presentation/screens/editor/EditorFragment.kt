package com.poe.poegps.feature.presentation.screens.editor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.permissionx.guolindev.PermissionX
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentEditorBinding
import com.poe.poegps.feature.data.remote.utils.MyLocationListener
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.adapter.OprAdapter
import com.poe.poegps.feature.presentation.screens.editor.dialog.*
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow
import java.util.Locale

@AndroidEntryPoint
class EditorFragment : Fragment(), MenuProvider, OnClickListener, MyLocationListener,
    AddPillarDialogFragment.Listener,
    AddPillarFromSaved10.Listener,
    ChoosePillarFrom04DialogFragment.Listener,
    ChoosePillarFrom10DialogFragment.Listener,
    AddPillarFromSaved04.Listener,
    AddPillarFromTp.Listener,
    OprAdapter.OnOprClickListener,
    OnCreateOtpDialogFragment.Listener,
    ActivityCompat.OnRequestPermissionsResultCallback{

    private var binding: FragmentEditorBinding by autoCleaned()
    private var adapter: OprAdapter by autoCleaned()
    private val viewModel by viewModels<EditorViewModel>()
    private val args: EditorFragmentArgs by lazy {
        EditorFragmentArgs.fromBundle(requireArguments())
    }

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
        binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.backBtn.setOnClickListener(this)
        setupPillarAdapter()
        collectViewModel()
        viewModel.getParentName(args.tplnr)
        requireActivity().addMenuProvider(this)
        checkEnabled()
        setupPillarLongClickMenuDialog()
    }

    private fun collectViewModel() {
        collectLifecycleFlow(viewModel.oprDisplayable) { list ->
            adapter.submitList(list)
        }
        collectLifecycleFlow(viewModel.lineName) { lineName ->
            binding.lineName.text = lineName
        }
    }

    private fun setupPillarAdapter() {
        val linearLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        adapter = OprAdapter()
        binding.coordinatesRv.adapter = adapter
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.coordinatesRv.layoutManager = linearLayoutManager
        adapter.onOprClickListener = this
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_editor, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.add_opr -> {
                /*findNavController().navigate(EditorFragmentDirections.actionEditorFragmentToAddPillarDialogFragment(
                    args.tplnr,
                    adapter.itemCount + 1
                   // requireContext().resources.getStringArray(R.array.wires)
                ))*/
                openCreatePillarDialog()
                true
            }
            R.id.add_savedopr -> {
                val dialog = AddPillarFromSaved10.newInstance()
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedPillar10DialogFragment")
                true
            }
            R.id.add_savedopr_from_small -> {
                val dialog = AddPillarFromSaved04.newInstance()
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedPillar04DialogFragment")
                true
            }
            R.id.add_savedtp_from_small -> {
                val dialog = AddPillarFromTp.newInstance(args.pltxt, args.tplnr, args.category, false)
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedTp04DialogFragment")
                true
            }
            R.id.add_saved_abon_tp_from_small -> {
                val dialog = AddPillarFromTp.newInstance(args.pltxt, args.tplnr, args.category, true)
                dialog.setListener(this)
                dialog.show(childFragmentManager, "CreateSavedTp04DialogFragment")
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().removeMenuProvider(this)
    }

    private fun openCreatePillarDialog() {
        val dialog = AddPillarDialogFragment.newInstance(
            args.tplnr,
            binding.lineName.text.toString(),
            args.category,
            args.isAbon
        )
        dialog.setListener(this)
        dialog.show(childFragmentManager, "CreatePillarDialogFragment")
    }

    private fun openChoosePillarFrom04Dialog(obj: ObjectDisplayable, tplnr: String) {
        val dialog = ChoosePillarFrom04DialogFragment.newInstance(
            obj.tplnr,
            tplnr,
            args.pltxt
        )
        dialog.setListener(this)
        dialog.show(childFragmentManager, "ChoosePillarFrom04DialogFragment")
    }

    private fun openChoosePillarFrom10Dialog(obj: ObjectDisplayable, tplnr: String) {
        val dialog = ChoosePillarFrom10DialogFragment.newInstance(
            obj.tplnr,
            tplnr,
            args.pltxt
        )
        dialog.setListener(this)
        dialog.show(childFragmentManager, "ChoosePillarFrom04DialogFragment")
    }

    /*private fun onCreateStartDialog() {
        if (adapter.itemCount == 0) {
            val dialog = AddPillarFromTp.newInstance(args.pltxt, args.tplnr)
            dialog.setListener(this)
            dialog.show(childFragmentManager, "CreateSavedTp04DialogFragment")
        }
    }*/


    override fun onSavedPillar04Adding(obj: ObjectDisplayable) {
        openChoosePillarFrom04Dialog(obj, args.tplnr)
    }

    override fun onSavedPillar10Adding(obj: ObjectDisplayable) {
        openChoosePillarFrom10Dialog(obj, args.tplnr)
    }

    override fun onPillarAdded(pillar: OprDisplayable) {
        viewModel.addPillarToDisplay(pillar)
    }

    /*override fun onSavedTpAdded(obj: OprDisplayable) {
        viewModel.addPillarToDisplay(obj)
    }*/

    /*override fun onPillarChoose(pillar: OprDisplayable) {
        viewModel.addPillarToDisplay(pillar)
    }*/

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.back_btn -> {
                findNavController().popBackStack()
            }
        }
    }

    override fun onTakeCoordinatesClick(opr: OprDisplayable, position: Int) {
        if (binding.include.GPSLatitude.text != "0.0" && locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            //viewModel.saveCoordinates(opr)
            opr.lat = binding.include.GPSLatitude.text.toString()
            opr.lng = binding.include.GPSLongitude.text.toString()
            viewModel.saveCoordinates(opr)
            adapter.notifyItemChanged(position)
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

    override fun onLongClick(opr: OprDisplayable) {
        OnPillarLongClickDialogFragment.show(childFragmentManager, opr)
        /*val dialog = PillarLongClickDialogFragment.newInstance(opr)
        dialog.show(childFragmentManager, "CreatePillarDialogFragment")*/
    }

    private fun setupPillarLongClickMenuDialog() {
        OnPillarLongClickDialogFragment.setupListeners(childFragmentManager, viewLifecycleOwner) {
            pillar, which ->
            when (which) {
                getString(R.string.createOtp) -> {
                    val dialog =  OnCreateOtpDialogFragment.newInstance(pillar)
                    dialog.setListener(this)
                    dialog.show(childFragmentManager, "CreateOtpaykaDialogFragment")
                }
                getString(R.string.clear_coord) -> {
                    viewModel.clearCoord(pillar)
                }
                getString(R.string.deleteOpr) -> {
                    viewModel.deleteOpr(pillar)
                }
            }
        }
    }

    override fun onOtpCreated(pillarStart: OprDisplayable, pillarSecond: OprDisplayable) {
        viewModel.createOtp(pillarStart, pillarSecond)
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
                            LocationManager.GPS_PROVIDER, EVERY_SECOND,
                            EVERY_0M, this
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

    companion object {
        const val EVERY_SECOND = 2000L
        const val EVERY_0M = 0f
    }

}