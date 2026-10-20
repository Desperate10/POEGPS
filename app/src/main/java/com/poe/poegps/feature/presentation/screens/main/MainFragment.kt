package com.poe.poegps.feature.presentation.screens.main

import androidx.compose.foundation.layout.navigationBarsPadding
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.poe.poegps.R
import com.poe.poegps.core.designsystem.PoeTheme
import com.poe.poegps.core.update.AppUpdater
import com.poe.poegps.feature.data.remote.model.upload.UploadState
import com.poe.poegps.feature.data.remote.utils.FilialManager
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.presentation.CoroutinesErrorHandler
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    private val viewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var filialManager: FilialManager

    @Inject
    lateinit var updater: AppUpdater

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { PoeTheme { MainRoute() } }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initialize()
        collectLogin()
    }

    override fun onResume() {
        super.onResume()
        // also catches a Play download that finished while the app was in the background
        updater.check()
    }

    @Composable
    private fun MainRoute() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val sync by viewModel.sync.collectAsStateWithLifecycle()
        val newLines by viewModel.newLines.collectAsStateWithLifecycle()
        val filials = stringArrayResource(R.array.filials).map(Filial::parse)
        val login by viewModel.login.collectAsStateWithLifecycle()
        val downloadedAt by filialManager.getDownloadedAt().collectAsStateWithLifecycle(null)
        val snackbar = remember { SnackbarHostState() }
        val update by updater.state.collectAsStateWithLifecycle()
        var updateOpen by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            viewModel.message.collect { snackbar.showSnackbar(it) }
        }
        LaunchedEffect(state.download) {
            when (val d = state.download) {
                UploadState.Complete -> {
                    viewModel.setUploadStateToIdle()
                    snackbar.showSnackbar("Дані філії оновлено")
                }
                is UploadState.Error -> {
                    viewModel.setUploadStateToIdle()
                    snackbar.showSnackbar("Помилка завантаження даних: ${d.message}")
                }
                else -> {}
            }
        }

        LaunchedEffect(sync) {
            // the server may have refused the upload as coming from an outdated version
            if (sync is SyncUiState.Failed) updater.check(force = true)
        }

        Box(Modifier.fillMaxSize()) {
            ObjectsScreen(
                state = state,
                filials = filials,
                uploading = sync == SyncUiState.Uploading,
                onTypeChange = viewModel::selectedObjectType,
                onQueryChange = viewModel::setQuery,
                onOpen = ::navigateToEditObjectFragment,
                onUploadOne = { viewModel.upload(it.tplnr) },
                onDeleteTap = { viewModel.deleteObject(it.name) },
                onClearLine = viewModel::clearLine,
                clearSummary = viewModel::clearLineSummary,
                onDownload = viewModel::loadObjectsToDb,
                onUploadAll = { viewModel.upload() },
                onFilialSelected = viewModel::selectedFilial,
                downloadedAt = downloadedAt,
                askFilial = login == null,
                banner = {
                    update?.takeIf { !it.required }?.let { u ->
                        UpdateBanner(u, onOpen = { updateOpen = true }, onDismiss = updater::dismiss)
                    }
                },
            )
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
            ) { data ->
                Snackbar(
                    data,
                    containerColor = PoeTheme.colors.inverse,
                    contentColor = PoeTheme.colors.onInverse,
                )
            }
            newLines?.let { p ->
                NewLinesDialog(p, onConfirm = viewModel::confirmNewLines, onCancel = viewModel::cancelNewLines)
            }
            sync?.let { s ->
                BackHandler { viewModel.closeSync() }
                SyncScreen(s, onDone = viewModel::closeSync, onRetry = viewModel::retryUpload)
            }
            login?.let { l ->
                LoginScreen(
                    state = l,
                    filials = filials,
                    savedFilial = state.filial,
                    onSubmit = viewModel::login,
                    onExit = { requireActivity().finish() },
                )
            }
            update?.let { u ->
                if (u.required) {
                    RequiredUpdateScreen(u, state.unsent, onProceed = { updater.proceed(requireActivity()) })
                } else if (updateOpen) {
                    UpdateSheet(u, state.unsent, onProceed = { updater.proceed(requireActivity()) }, onClose = { updateOpen = false })
                }
            }
        }
    }

    private fun collectLogin() {
        collectLifecycleFlow(viewModel.tokenValidity) { tokenResponse ->
            when (tokenResponse) {
                is ApiResponse.Success -> if (tokenResponse.data.error) viewModel.requireLogin()
                is ApiResponse.Error ->
                    Toast.makeText(requireContext(), "Error! ${tokenResponse.message}", Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    private fun navigateToEditObjectFragment(obj: ObjectDisplayable) {
        val type = viewModel.spinnerObjectType.value
        val directions = if (type == ObjectType.TP || type == ObjectType.TPABON) {
            MainFragmentDirections.actionMainFragmentToTpEditorFragment(
                obj.tplnr, obj.name, type, obj.category, obj.isAbon, obj.pillarType
            )
        } else {
            MainFragmentDirections.actionMainFragmentToEditorFragment(
                obj.tplnr, obj.name, type, obj.category, obj.isAbon, obj.pillarType
            )
        }
        findNavController().navigate(directions)
    }
}
