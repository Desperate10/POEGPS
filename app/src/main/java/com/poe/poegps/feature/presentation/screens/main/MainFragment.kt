package com.poe.poegps.feature.presentation.screens.main

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
import com.poe.poegps.feature.data.remote.model.upload.UploadState
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.presentation.CoroutinesErrorHandler
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType
import com.poe.poegps.feature.presentation.screens.main.dialog.LoginDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class MainFragment : Fragment(), LoginDialogFragment.Listener {

    private val viewModel by viewModels<MainViewModel>()

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

    @Composable
    private fun MainRoute() {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val sync by viewModel.sync.collectAsStateWithLifecycle()
        val filials = stringArrayResource(R.array.filials).map(Filial::parse)
        val snackbar = remember { SnackbarHostState() }

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
                onDownload = viewModel::loadObjectsToDb,
                onUploadAll = { viewModel.upload() },
                onFilialSelected = viewModel::selectedFilial,
            )
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
            ) { data ->
                Snackbar(
                    data,
                    containerColor = PoeTheme.colors.inverse,
                    contentColor = PoeTheme.colors.onInverse,
                )
            }
            sync?.let { s ->
                BackHandler { viewModel.closeSync() }
                SyncScreen(s, onDone = viewModel::closeSync, onRetry = viewModel::retryUpload)
            }
        }
    }

    // B1 (login) is redesigned later; until then the old dialog stays
    private fun collectLogin() {
        collectLifecycleFlow(viewModel.loginResponse) { loginResponse ->
            when (loginResponse) {
                is ApiResponse.Error -> {
                    Toast.makeText(requireContext(), loginResponse.message, Toast.LENGTH_SHORT).show()
                    showLogin()
                }
                ApiResponse.Loading -> {}
                is ApiResponse.Success -> {
                    Toast.makeText(requireContext(), "Вас авторизовано!", Toast.LENGTH_SHORT).show()
                    viewModel.setTokenValidity()
                    viewModel.setLoginResponseToLoading()
                    viewModel.saveToken(loginResponse.data.jwt)
                }
            }
        }
        collectLifecycleFlow(viewModel.tokenValidity) { tokenResponse ->
            when (tokenResponse) {
                is ApiResponse.Success -> if (tokenResponse.data.error) showLogin()
                is ApiResponse.Error ->
                    Toast.makeText(requireContext(), "Error! ${tokenResponse.message}", Toast.LENGTH_SHORT).show()
                else -> {}
            }
        }
    }

    private fun showLogin() {
        if (childFragmentManager.findFragmentByTag("login") != null) return
        val dialog = LoginDialogFragment()
        dialog.setListener(this)
        dialog.show(childFragmentManager, "login")
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

    override fun onLoginSuccess(login: String, password: String) {
        viewModel.authorization(
            login,
            password,
            object : CoroutinesErrorHandler {
                override fun onError(message: String) {
                    Toast.makeText(context, "Error! $message", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
