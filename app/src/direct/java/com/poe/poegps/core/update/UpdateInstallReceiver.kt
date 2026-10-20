package com.poe.poegps.core.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.core.content.IntentCompat
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** PackageInstaller's answer to [DirectAppUpdater]'s install session. */
class UpdateInstallReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun updater(): DirectAppUpdater
    }

    override fun onReceive(context: Context, intent: Intent) {
        val updater = EntryPointAccessors.fromApplication(context, Deps::class.java).updater()
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                // Android's "Update this app?" dialog; the app is in front, the crew just tapped "Встановити"
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)?.let {
                    context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                updater.awaitingConfirmation()
            }
            // the process is replaced by the new version right after this
            PackageInstaller.STATUS_SUCCESS -> {}
            else -> updater.installFailed(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
        }
    }
}
