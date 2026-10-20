package com.poe.poegps.core.update

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.poe.poegps.BuildConfig
import com.poe.poegps.feature.data.remote.model.AppReleaseDTO
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * The sideloaded build updates itself from the GIS server: GET app/release, download the APK into the
 * cache (resuming a dropped download), check its SHA-256, package and signing key, then hand it to
 * PackageInstaller. Android asks the crew to confirm, except when this app installed itself last time.
 */
@Singleton
class DirectAppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AppReleaseApi,
    private val retrofit: Retrofit,
    client: OkHttpClient,
) : AppUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = client.newBuilder().readTimeout(60, TimeUnit.SECONDS).build()
    private val dir = File(context.cacheDir, "update")

    private val _state = MutableStateFlow<UpdateState?>(null)
    override val state: StateFlow<UpdateState?> = _state.asStateFlow()

    private var release: AppReleaseDTO? = null
    /** The downloaded APK whose hash, package and key were checked. */
    private var verified: File? = null
    private var lastServerCheck = 0L
    private var dismissedCode = 0
    private var job: Job? = null

    override fun check(force: Boolean) {
        if (job?.isActive == true) return
        val now = SystemClock.elapsedRealtime()
        if (!force && lastServerCheck != 0L && now - lastServerCheck < UPDATE_CHECK_INTERVAL_MS) return
        lastServerCheck = now
        job = scope.launch {
            val response = try {
                api.release()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // offline in the field is normal: keep whatever was offered
                lastServerCheck = 0L
                return@launch
            }
            release = when {
                response.isSuccessful -> response.body()
                response.code() == 404 -> null  // nothing released
                else -> return@launch
            }
            // installed already (or withdrawn): the downloaded APK is dead weight
            if (release.let { it == null || it.versionCode <= BuildConfig.VERSION_CODE }) {
                verified = null
                dir.deleteRecursively()
            }
            _state.value = offer()
        }
    }

    private fun offer(): UpdateState? {
        val r = release ?: return null
        val required = BuildConfig.VERSION_CODE < r.minVersionCode
        val newer = r.versionCode > BuildConfig.VERSION_CODE && r.apk != null && r.sha256 != null
        if (!newer && !required) return null
        if (!required && r.versionCode == dismissedCode) return null
        val ready = newer && verified?.let { it.isFile && it.name == apkName(r) } == true
        return UpdateState(
            versionName = r.versionName,
            notes = r.notes.orEmpty(),
            required = required,
            sizeBytes = r.size,
            phase = if (ready) UpdatePhase.Ready else UpdatePhase.Available,
            unavailable = !newer,
        )
    }

    override fun proceed(activity: Activity) {
        val s = _state.value ?: return
        when (s.phase) {
            UpdatePhase.Available -> if (!s.unavailable) startDownload()
            UpdatePhase.Ready -> install(activity)
            UpdatePhase.Downloading, UpdatePhase.Installing -> {}
        }
    }

    override fun dismiss() {
        val s = _state.value ?: return
        if (s.required || s.phase != UpdatePhase.Available) return
        dismissedCode = release?.versionCode ?: 0
        _state.value = null
    }

    private fun startDownload() {
        val r = release ?: return
        if (job?.isActive == true) return
        _state.update { it?.copy(phase = UpdatePhase.Downloading, progress = null, error = null) }
        job = scope.launch {
            try {
                verified = download(r)
                _state.value = offer()
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApkProblem) {
                fail(e.message)
            } catch (e: IOException) {
                Log.w(TAG, "download failed", e)
                fail("Завантаження перервалося. Перевірте інтернет і натисніть ще раз: продовжиться з того ж місця.")
            } catch (e: Exception) {
                Log.w(TAG, "download failed", e)
                fail("Не вдалося завантажити оновлення: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }
    }

    private fun fail(message: String?) {
        _state.value = offer()?.copy(error = message)
    }

    private fun apkName(r: AppReleaseDTO) = "${r.sha256!!.lowercase()}.apk"

    private suspend fun download(r: AppReleaseDTO): File {
        dir.mkdirs()
        val file = File(dir, apkName(r))
        // earlier releases' downloads
        dir.listFiles()?.filter { it != file }?.forEach { it.delete() }

        val size = r.size
        var have = if (file.isFile) file.length() else 0L
        if (size != null && have > size) {
            file.delete()
            have = 0
        }
        if (size == null || have < size) {
            val url = retrofit.baseUrl().resolve(r.apk!!) ?: throw ApkProblem("Невірна адреса оновлення на сервері")
            val request = Request.Builder().url(url)
                .apply { if (have > 0) header("Range", "bytes=$have-") }
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                // 200 instead of 206: the server sent the whole file again
                if (response.code != 206) have = 0
                val body = response.body ?: throw IOException("empty body")
                val total = size ?: body.contentLength().takeIf { it >= 0 }?.plus(have)
                FileOutputStream(file, have > 0).use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        var done = have
                        var shown = -1
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total != null && total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != shown) {
                                    shown = pct
                                    _state.update { it?.copy(progress = done.toFloat() / total) }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!sha256(file).equals(r.sha256, ignoreCase = true)) {
            file.delete()
            throw ApkProblem("Файл пошкоджено під час завантаження. Натисніть ще раз, щоб завантажити заново.")
        }
        checkApk(file)
        return file
    }

    /** What PackageInstaller would refuse anyway, but in words the crew understands, before they confirm. */
    private fun checkApk(file: File) {
        val pm = context.packageManager
        val archive = archiveInfo(pm, file) ?: run {
            file.delete()
            throw ApkProblem("Завантажений файл не є застосунком. Повідомте адміністратора.")
        }
        if (archive.packageName != context.packageName) {
            file.delete()
            throw ApkProblem("На сервері викладено інший застосунок (${archive.packageName}). Повідомте адміністратора.")
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <= BuildConfig.VERSION_CODE) {
            file.delete()
            throw ApkProblem("На сервері та сама або старіша версія. Повідомте адміністратора.")
        }
        val installed = signers(installedInfo(pm))
        val incoming = signers(archive)
        if (installed.isNotEmpty() && incoming.isNotEmpty() && !incoming.containsAll(installed)) {
            file.delete()
            throw ApkProblem(
                "Оновлення підписане іншим ключем, ніж цей застосунок (наприклад, тестова збірка), тож Android його не встановить. " +
                    "Повідомте адміністратора."
            )
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private fun install(activity: Activity) {
        val file = verified?.takeIf { it.isFile } ?: run {
            _state.value = offer()
            return
        }
        val pm = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !pm.canRequestPackageInstalls()) {
            _state.update { it?.copy(error = "Дозвольте POEGPS встановлювати застосунки: увімкніть перемикач, поверніться назад і натисніть «Встановити» ще раз.") }
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            return
        }
        _state.update { it?.copy(phase = UpdatePhase.Installing, error = null) }
        scope.launch {
            try {
                val installer = pm.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(context.packageName)
                    setSize(file.length())
                    // no confirmation when this app was also the one that installed itself last time
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }
                val id = installer.createSession(params)
                installer.openSession(id).use { session ->
                    session.openWrite("base.apk", 0, file.length()).use { out ->
                        file.inputStream().use { it.copyTo(out) }
                        session.fsync(out)
                    }
                    // the installer adds the status to the intent, so it must stay mutable
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
                    val callback = PendingIntent.getBroadcast(
                        context, id, Intent(context, UpdateInstallReceiver::class.java), flags
                    )
                    session.commit(callback.intentSender)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "install failed", e)
                installFailed(PackageInstaller.STATUS_FAILURE, e.localizedMessage)
            }
        }
    }

    /** Android shows its confirmation; if the crew backs out of it, the button must work again. */
    internal fun awaitingConfirmation() {
        _state.update { it?.copy(phase = UpdatePhase.Ready) }
    }

    internal fun installFailed(status: Int, message: String?) {
        Log.w(TAG, "install status $status: $message")
        val text = when (status) {
            PackageInstaller.STATUS_FAILURE_ABORTED -> null  // the crew cancelled
            PackageInstaller.STATUS_FAILURE_STORAGE -> "Замало місця на телефоні для встановлення."
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                "Android не дозволив замінити застосунок (інший ключ підпису). Повідомте адміністратора."
            PackageInstaller.STATUS_FAILURE_BLOCKED -> "Встановлення заблоковано налаштуваннями телефону."
            else -> "Не вдалося встановити оновлення${message?.let { ": $it" } ?: ""}"
        }
        _state.update { it?.copy(phase = UpdatePhase.Ready, error = text) }
    }

    private class ApkProblem(message: String) : Exception(message)

    companion object {
        private const val TAG = "DirectAppUpdater"

        @Suppress("DEPRECATION")
        private val SIGNATURE_FLAGS =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
            else PackageManager.GET_SIGNATURES

        private fun archiveInfo(pm: PackageManager, file: File): PackageInfo? =
            pm.getPackageArchiveInfo(file.path, SIGNATURE_FLAGS)

        @Suppress("DEPRECATION")
        private fun installedInfo(pm: PackageManager): PackageInfo =
            pm.getPackageInfo(BuildConfig.APPLICATION_ID, SIGNATURE_FLAGS)

        /** SHA-256 of every certificate the package is or was signed with (key rotation keeps the old ones). */
        @Suppress("DEPRECATION")
        private fun signers(info: PackageInfo): Set<String> {
            val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val s = info.signingInfo ?: return emptySet()
                if (s.hasMultipleSigners()) s.apkContentsSigners else s.signingCertificateHistory
            } else {
                info.signatures
            }
            return certs.orEmpty().map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }.toSet()
        }

        private fun sha256(file: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                }
            }
            return hex(md.digest())
        }

        private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }
    }
}
