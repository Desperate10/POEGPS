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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.poe.poegps.feature.presentation.capture.ReadyChecklist
import com.poe.poegps.feature.presentation.map.MapGisPole
import com.poe.poegps.feature.presentation.capture.requestLocation
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.RecloserType
import com.poe.poegps.feature.data.remote.utils.FilialManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.Locale

@AndroidEntryPoint
class EditorFragment : Fragment(), PoleActions, AddActions {

    private val viewModel by viewModels<EditorViewModel>()
    private val args: EditorFragmentArgs by lazy {
        EditorFragmentArgs.fromBundle(requireArguments())
    }

    @Inject
    lateinit var filialManager: FilialManager

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
        val gisMerge by viewModel.gisMerge.collectAsStateWithLifecycle()
        val downloadedAt by filialManager.getDownloadedAt().collectAsStateWithLifecycle(null)
        // B12 on entry while GPS isn't ready, and again from the GPS chip
        var showReady by rememberSaveable { mutableStateOf(true) }

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
                gisPoles = gisPoles.map { MapGisPole(it.name, it.lat.toDouble(), it.lng.toDouble()) },
                onBack = { findNavController().popBackStack() },
                onSelect = viewModel::selectPole,
                onGpsProblem = { showReady = true },
                actions = this@EditorFragment,
                adds = this@EditorFragment,
            )
            CaptureFeedback(
                results = viewModel.captureResult,
                messages = merge(viewModel.notices, viewModel.insertResultFlow.map {
                    when (it) {
                        InsertResult.Success -> "Опору збережено"
                        InsertResult.Duplicate -> "Опора з такою назвою вже існує"
                        InsertResult.Failure -> "Помилка створення нової опори. Зверніться до адміністратора"
                    }
                }),
                onConfirmed = { c ->
                    val manual = c.manual
                    if (manual != null) viewModel.placeManually(c.pole, manual.first, manual.second, confirmed = true)
                    else viewModel.capture(c.pole, belowGate = c.belowGate, confirmed = true)
                },
            )
            // after the GPS checklist: both on entry would stack
            if (!showReady) {
                gisMerge?.let { offer ->
                    GisMergeDialog(offer, onAccept = viewModel::acceptGisMerge, onDecline = viewModel::declineGisMerge)
                }
            }
            ReadyChecklist(
                show = showReady,
                gps = gps,
                downloadedAt = downloadedAt,
                onGrant = { requestLocation(viewModel::restartGps) },
                onDismiss = { showReady = false },
                onBack = { findNavController().popBackStack() },
            )
        }
    }

    // B4 actions
    override fun capture(pole: OprDisplayable, belowGate: Boolean) = viewModel.capture(pole, belowGate)

    override fun clearCoordinates(pole: OprDisplayable) = viewModel.clearCoord(pole)

    override fun delete(pole: OprDisplayable) = viewModel.deleteOpr(pole)

    override fun placeManually(pole: OprDisplayable, lat: Double, lng: Double) = viewModel.placeManually(pole, lat, lng)

    override fun gisPosition(pole: OprDisplayable): Pair<Double, Double>? =
        viewModel.gisPositionOf(pole)?.let { it.lat.toDouble() to it.lng.toDouble() }

    override fun restoreGisPosition(pole: OprDisplayable) = viewModel.restoreGisPosition(pole)

    // B8–B11
    override fun addPole(name: String, afterId: Int?) = viewModel.addPole(name, afterId)

    override fun addExisting(source: PickSource, item: PickItem, afterId: Int?) = viewModel.addExisting(source, item, afterId)

    override fun lines(source: PickSource): Flow<List<ObjectDisplayable>> = viewModel.getLines(source)

    override fun candidates(source: PickSource, line: ObjectDisplayable?): Flow<List<PickItem>> =
        viewModel.candidates(source, line)

    override suspend fun nearby(source: PickSource, lat: Double, lng: Double): List<PickItem> =
        viewModel.nearby(source, lat, lng)

    override fun createTap(from: OprDisplayable, firstPole: String, open: Boolean, gisName: String?) =
        viewModel.createTap(from, firstPole, open, gisName)

    override fun saveRecloser(pole: OprDisplayable, type: RecloserType, name: String) =
        viewModel.saveRecloser(pole, type, name)
}
