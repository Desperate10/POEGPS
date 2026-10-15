package com.poe.poegps.feature.presentation.screens.tpeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.feature.presentation.capture.CaptureFeedback
import com.poe.poegps.feature.presentation.capture.fixGps
import com.poe.poegps.feature.presentation.capture.requestLocation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TpEditorFragment : Fragment() {

    private val args: TpEditorFragmentArgs by lazy {
        TpEditorFragmentArgs.fromBundle(requireArguments())
    }

    private val viewModel by viewModels<TpEditorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocation(viewModel::restartGps)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            PoeTheme {
                val tp by viewModel.tp.collectAsStateWithLifecycle()
                val progress by viewModel.progress.collectAsStateWithLifecycle()
                val meta by viewModel.captureMeta.collectAsStateWithLifecycle()
                val gps by viewModel.gps.collectAsStateWithLifecycle()
                val trust by viewModel.trust.collectAsStateWithLifecycle()
                val gisLines by viewModel.gisLines.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize()) {
                    TpEditorScreen(
                        tp = tp,
                        title = args.pltxt,
                        tplnr = args.tplnr,
                        progress = progress,
                        meta = meta,
                        gps = gps,
                        trust = trust,
                        gisLines = gisLines,
                        onBack = { findNavController().popBackStack() },
                        onCapture = { belowGate -> viewModel.capture(belowGate) },
                        onPlace = { lat, lng -> viewModel.placeManually(lat, lng) },
                        onGpsProblem = { fixGps(viewModel.gps.value.permission, viewModel::restartGps) },
                    )
                    CaptureFeedback(
                        results = viewModel.captureResult,
                        onConfirmed = { c ->
                            val manual = c.manual
                            if (manual != null) viewModel.placeManually(manual.first, manual.second, confirmed = true)
                            else viewModel.capture(belowGate = c.belowGate, confirmed = true)
                        },
                    )
                }
            }
        }
    }
}
