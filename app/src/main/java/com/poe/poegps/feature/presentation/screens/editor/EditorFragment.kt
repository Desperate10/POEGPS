package com.poe.poegps.feature.presentation.screens.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.poe.poegps.feature.presentation.capture.CaptureFeedback
import com.poe.poegps.feature.presentation.capture.fixGps
import com.poe.poegps.feature.presentation.capture.requestLocation
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.RecloserDisplayable
import com.poe.poegps.feature.presentation.screens.editor.dialog.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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
        requestLocation(viewModel::restartGps)
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
        val trust by viewModel.trust.collectAsStateWithLifecycle()
        val captureMeta by viewModel.captureMeta.collectAsStateWithLifecycle()

        Box(Modifier.fillMaxSize()) {
            EditorScreen(
                tplnr = args.tplnr,
                lineName = lineName,
                category = args.category,
                poles = poles,
                selectedId = selected,
                gps = gps,
                trust = trust,
                captureMeta = captureMeta,
                gisLines = gisLines,
                gisPoles = gisPoles.map { it.lat.toDouble() to it.lng.toDouble() },
                onBack = { findNavController().popBackStack() },
                onAdd = ::openAddDialog,
                onSelect = viewModel::selectPole,
                onGpsProblem = { fixGps(viewModel.gps.value.permission, viewModel::restartGps) },
                actions = this@EditorFragment,
            )
            CaptureFeedback(
                results = viewModel.captureResult,
                messages = viewModel.insertResultFlow.map {
                    when (it) {
                        InsertResult.Success -> "Опору збережено"
                        InsertResult.Duplicate -> "Опора з такою назвою вже існує"
                        InsertResult.Failure -> "Помилка створення нової опори. Зверніться до адміністратора"
                    }
                },
                onConfirmed = { c ->
                    val manual = c.manual
                    if (manual != null) viewModel.placeManually(c.pole, manual.first, manual.second, confirmed = true)
                    else viewModel.capture(c.pole, belowGate = c.belowGate, confirmed = true)
                },
            )
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
    override fun capture(pole: OprDisplayable, belowGate: Boolean) = viewModel.capture(pole, belowGate)

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

    override fun placeManually(pole: OprDisplayable, lat: Double, lng: Double) = viewModel.placeManually(pole, lat, lng)
}
