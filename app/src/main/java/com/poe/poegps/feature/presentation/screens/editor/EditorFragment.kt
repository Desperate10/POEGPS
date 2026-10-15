package com.poe.poegps.feature.presentation.screens.editor

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.permissionx.guolindev.PermissionX
import com.poe.poegps.R
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.RecloserDisplayable
import com.poe.poegps.feature.presentation.screens.editor.dialog.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import java.util.Locale

@AndroidEntryPoint
class EditorFragment : Fragment(),
    AddPillarDialogFragment.Listener,
    AddPillarFromSaved10.Listener,
    ChoosePillarFrom04DialogFragment.Listener,
    ChoosePillarFrom10DialogFragment.Listener,
    AddPillarFromSaved04.Listener,
    AddPillarFromTp.Listener,
    OnCreateOtpDialogFragment.Listener,
    OnAddRecloserDialogFragment.Listener,
    PoleActions {

    private val viewModel by viewModels<EditorViewModel>()
    private val args: EditorFragmentArgs by lazy {
        EditorFragmentArgs.fromBundle(requireArguments())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { PoeTheme { EditorRoute() } }
    }

    @Composable
    private fun EditorRoute() {
        val poles by viewModel.oprDisplayable.collectAsStateWithLifecycle()
        val lineName by viewModel.lineName.collectAsStateWithLifecycle()
        val selected by viewModel.selectedPoleId.collectAsStateWithLifecycle()
        val gps by viewModel.gps.collectAsStateWithLifecycle()
        val gisLines by viewModel.gisLines.collectAsStateWithLifecycle()
        val gisPoles by viewModel.gisPoles.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
            merge(
                viewModel.insertResultFlow.map {
                    when (it) {
                        InsertResult.Success -> "Опору збережено"
                        InsertResult.Duplicate -> "Опора з такою назвою вже існує"
                        InsertResult.Failure -> "Помилка створення нової опори. Зверніться до адміністратора"
                    }
                },
                viewModel.captureResult.map {
                    when (it) {
                        is CaptureResult.Captured ->
                            "${it.pole}: координати знято" + (it.accuracy?.let { a -> String.format(Locale.US, " · ±%.1f м", a) } ?: "")
                        CaptureResult.NoFix -> "Немає позиції GPS — зачекайте на супутники"
                    }
                },
            ).collect { snackbar.showSnackbar(it) }
        }

        Box(Modifier.fillMaxSize()) {
            EditorScreen(
                tplnr = args.tplnr,
                lineName = lineName,
                category = args.category,
                poles = poles,
                selectedId = selected,
                gps = gps,
                gisLines = gisLines,
                gisPoles = gisPoles.map { it.lat.toDouble() to it.lng.toDouble() },
                onBack = { findNavController().popBackStack() },
                onAdd = ::openAddDialog,
                onSelect = viewModel::selectPole,
                onGpsProblem = ::fixGps,
                actions = this@EditorFragment,
            )
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.TopCenter).padding(top = 112.dp, start = 16.dp, end = 16.dp),
            ) { data ->
                Snackbar(data, containerColor = PoeTheme.colors.inverse, contentColor = PoeTheme.colors.onInverse)
            }
        }
    }

    private fun openAddDialog(option: AddOption) {
        when (option) {
            AddOption.NEW -> AddPillarDialogFragment.newInstance(
                args.tplnr, viewModel.lineName.value, args.category, args.isAbon
            ).also { it.setListener(this) }.show(childFragmentManager, "CreatePillarDialogFragment")

            AddOption.EXISTING_10 -> AddPillarFromSaved10.newInstance(args.category)
                .also { it.setListener(this) }.show(childFragmentManager, "CreateSavedPillar10DialogFragment")

            AddOption.EXISTING_04 -> AddPillarFromSaved04.newInstance(args.category)
                .also { it.setListener(this) }.show(childFragmentManager, "CreateSavedPillar04DialogFragment")

            AddOption.TP, AddOption.ABON_TP -> AddPillarFromTp.newInstance(
                args.pltxt, args.tplnr, args.category, option == AddOption.ABON_TP
            ).also { it.setListener(this) }.show(childFragmentManager, "CreateSavedTp04DialogFragment")
        }
    }

    override fun onSavedPillar04Adding(obj: ObjectDisplayable) {
        ChoosePillarFrom04DialogFragment.newInstance(obj.tplnr, args.tplnr, args.pltxt)
            .also { it.setListener(this) }.show(childFragmentManager, "ChoosePillarFrom04DialogFragment")
    }

    override fun onSavedPillar10Adding(obj: ObjectDisplayable) {
        ChoosePillarFrom10DialogFragment.newInstance(obj.tplnr, args.tplnr, args.pltxt)
            .also { it.setListener(this) }.show(childFragmentManager, "ChoosePillarFrom04DialogFragment")
    }

    override fun onPillarAdded(pillar: OprDisplayable) {
        viewModel.addPillarToDisplay(pillar)
    }

    override fun onOtpCreated(pillarStart: OprDisplayable, pillarSecond: OprDisplayable) {
        viewModel.createOtp(pillarStart, pillarSecond)
    }

    override fun onRecloserCreated(recloser: RecloserDisplayable) {
        viewModel.createRecloser(recloser)
    }

    // B4 actions
    override fun capture(pole: OprDisplayable) = viewModel.capture(pole)

    override fun createTap(pole: OprDisplayable) {
        OnCreateOtpDialogFragment.newInstance(pole)
            .also { it.setListener(this) }.show(childFragmentManager, "CreateOtpaykaDialogFragment")
    }

    override fun addRecloser(pole: OprDisplayable) {
        OnAddRecloserDialogFragment.newInstance(pole)
            .also { it.setListener(this) }.show(childFragmentManager, "AddRecloserDialogFragment")
    }

    override fun clearCoordinates(pole: OprDisplayable) = viewModel.clearCoord(pole)

    override fun delete(pole: OprDisplayable) = viewModel.deleteOpr(pole)

    private fun fixGps() {
        if (!viewModel.gps.value.permission) checkPermissions()
        else LocationToggleDialogFragment.show(parentFragmentManager)
    }

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
            .request { allGranted, _, _ ->
                if (allGranted) {
                    viewModel.restartGps()
                    val manager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        LocationToggleDialogFragment.show(parentFragmentManager)
                    }
                }
                // denied: the GPS chip and the capture button say so and offer to ask again
            }
    }
}
