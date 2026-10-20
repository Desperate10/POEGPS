package com.poe.poegps.core.update

import android.app.Activity
import com.poe.poegps.feature.data.remote.model.AppReleaseDTO
import kotlinx.coroutines.flow.StateFlow
import retrofit2.Response
import retrofit2.http.GET

/**
 * Offers app updates from inside the app. The Play build asks Google Play (In-App Updates), the
 * direct build downloads the APK from the GIS server. Both read the server's minimum version: below
 * it the server refuses uploads, so the update is required.
 */
interface AppUpdater {
    /** null = nothing to offer (up to date, not checked yet, or offline). */
    val state: StateFlow<UpdateState?>

    /** Asks the server (and Play); at most every few minutes unless [force]. */
    fun check(force: Boolean = false)

    /** Available → download it (direct) or start Google Play's flow. Ready → install / restart. */
    fun proceed(activity: Activity)

    /** Hides an optional offer until the next launch. */
    fun dismiss()
}

data class UpdateState(
    /** The new version's name; null when Play doesn't tell it. */
    val versionName: String?,
    val notes: String = "",
    /** Below the server's minimum: uploads are refused until the update. */
    val required: Boolean = false,
    val sizeBytes: Long? = null,
    val phase: UpdatePhase = UpdatePhase.Available,
    /** 0..1 while downloading, null when the size is unknown. */
    val progress: Float? = null,
    /** The last attempt failed: why, in words for the crew. Proceeding retries. */
    val error: String? = null,
    /** Required, but there is nothing to install here (Play hasn't got it yet, the server has no APK). */
    val unavailable: Boolean = false,
    /** Google Play does the work: other button words ("Відкрити Google Play", restart instead of install). */
    val playStore: Boolean = false,
)

enum class UpdatePhase { Available, Downloading, Ready, Installing }

interface AppReleaseApi {
    // no Authorization: a phone whose login expired must still be able to update
    @GET("app/release")
    suspend fun release(): Response<AppReleaseDTO>
}

/** Checks closer together than this only repeat the last answer. */
const val UPDATE_CHECK_INTERVAL_MS = 15 * 60_000L
