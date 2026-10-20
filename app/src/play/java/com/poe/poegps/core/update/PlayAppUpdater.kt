package com.poe.poegps.core.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.poe.poegps.BuildConfig
import com.poe.poegps.feature.data.remote.model.AppReleaseDTO
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Play build: Google Play downloads and installs (In-App Updates). An optional update downloads in
 * the background (flexible) and is applied by a restart; a required one, below the GIS server's minimum,
 * uses Play's full-screen flow. The server's release notes are shown when its versionCode is Play's.
 */
@Singleton
class PlayAppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AppReleaseApi,
) : AppUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val manager = AppUpdateManagerFactory.create(context)

    private val _state = MutableStateFlow<UpdateState?>(null)
    override val state: StateFlow<UpdateState?> = _state.asStateFlow()

    private var info: AppUpdateInfo? = null
    private var release: AppReleaseDTO? = null
    private var lastServerCheck = 0L
    private var dismissedCode = 0
    private var job: Job? = null

    private val listener = InstallStateUpdatedListener { s ->
        when (s.installStatus()) {
            InstallStatus.PENDING -> _state.update { it?.copy(phase = UpdatePhase.Downloading, progress = null, error = null) }
            InstallStatus.DOWNLOADING -> {
                val total = s.totalBytesToDownload()
                val progress = if (total > 0) s.bytesDownloaded().toFloat() / total else null
                _state.update { it?.copy(phase = UpdatePhase.Downloading, progress = progress, error = null) }
            }
            InstallStatus.DOWNLOADED -> _state.update { it?.copy(phase = UpdatePhase.Ready, error = null) }
            InstallStatus.INSTALLING -> _state.update { it?.copy(phase = UpdatePhase.Installing) }
            InstallStatus.FAILED -> _state.update {
                it?.copy(phase = UpdatePhase.Available, error = "Google Play не зміг завантажити оновлення (код ${s.installErrorCode()}). Спробуйте ще раз.")
            }
            InstallStatus.CANCELED -> _state.update { it?.copy(phase = UpdatePhase.Available, progress = null) }
            else -> {}
        }
    }

    init {
        // the updater lives as long as the process
        manager.registerListener(listener)
    }

    override fun check(force: Boolean) {
        if (job?.isActive == true) return
        job = scope.launch {
            val now = SystemClock.elapsedRealtime()
            if (force || lastServerCheck == 0L || now - lastServerCheck >= UPDATE_CHECK_INTERVAL_MS) {
                lastServerCheck = now
                try {
                    val response = api.release()
                    if (response.isSuccessful) release = response.body()
                    else if (response.code() == 404) release = null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // offline: keep the last minimum
                    lastServerCheck = 0L
                }
            }
            // Play's answer is local and quick: always fresh, so a download finished meanwhile shows up
            info = try {
                manager.requestAppUpdateInfo()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // not installed from Play, no Play Store, …
                Log.i(TAG, "Play update info: ${e.message}")
                null
            }
            val keep = _state.value?.phase == UpdatePhase.Downloading || _state.value?.phase == UpdatePhase.Installing
            if (!keep) _state.value = offer()
        }
    }

    private fun offer(): UpdateState? {
        val i = info
        val r = release
        val required = r != null && BuildConfig.VERSION_CODE < r.minVersionCode
        val inPlay = i != null && (
            i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                i.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ||
                i.installStatus() == InstallStatus.DOWNLOADED
            )
        if (!inPlay && !required) return null
        val code = if (inPlay) i!!.availableVersionCode() else 0
        if (!required && code == dismissedCode) return null
        // the server describes the same version: its name and notes
        val described = r?.takeIf { it.versionCode == code || !inPlay }
        return UpdateState(
            versionName = described?.versionName,
            notes = described?.notes.orEmpty(),
            required = required,
            phase = when {
                !inPlay -> UpdatePhase.Available
                i!!.installStatus() == InstallStatus.DOWNLOADED -> UpdatePhase.Ready
                i.installStatus() == InstallStatus.DOWNLOADING || i.installStatus() == InstallStatus.PENDING -> UpdatePhase.Downloading
                else -> UpdatePhase.Available
            },
            unavailable = !inPlay,
            playStore = true,
        )
    }

    override fun proceed(activity: Activity) {
        val s = _state.value ?: return
        when (s.phase) {
            UpdatePhase.Ready -> manager.completeUpdate()  // installs and restarts the app
            UpdatePhase.Available -> if (s.unavailable) openStore(activity) else startFlow(activity, s.required)
            UpdatePhase.Downloading, UpdatePhase.Installing -> {}
        }
    }

    private fun startFlow(activity: Activity, required: Boolean) {
        val i = info ?: return
        val immediate = (required || i.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
            i.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val type = when {
            immediate -> AppUpdateType.IMMEDIATE
            i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
            i.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
            else -> {
                openStore(activity)
                return
            }
        }
        _state.update { it?.copy(error = null) }
        manager.startUpdateFlow(i, activity, AppUpdateOptions.newBuilder(type).build())
            .addOnFailureListener { e ->
                Log.w(TAG, "update flow failed", e)
                _state.update { it?.copy(error = "Google Play не відкрив оновлення. Спробуйте ще раз.") }
            }
        // the AppUpdateInfo is spent once a flow started: the next proceed needs a fresh one
        check()
    }

    /** Play has no newer version for this phone yet (or isn't the installer): its store page. */
    private fun openStore(activity: Activity) {
        val id = context.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).setPackage("com.android.vending"))
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")))
        }
    }

    override fun dismiss() {
        val s = _state.value ?: return
        if (s.required || s.phase != UpdatePhase.Available) return
        dismissedCode = info?.availableVersionCode() ?: 0
        _state.value = null
    }

    private companion object {
        const val TAG = "PlayAppUpdater"
    }
}
