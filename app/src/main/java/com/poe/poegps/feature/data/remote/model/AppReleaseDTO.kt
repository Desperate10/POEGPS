package com.poe.poegps.feature.data.remote.model

/** GET app/release: the APK phones without Google Play should run, and the oldest version /save accepts. */
data class AppReleaseDTO(
    val versionCode: Int = 0,
    val versionName: String? = null,
    val minVersionCode: Int = 0,
    val notes: String? = null,
    /** Relative to the API base URL; null when the server has the version guard only, no APK. */
    val apk: String? = null,
    val size: Long? = null,
    val sha256: String? = null,
)
