package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.poe.poegps.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocationToggleDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setMessage("Ввімкнути GPS?").setCancelable(false)
            .setPositiveButton(getString(R.string.yes)) { _: DialogInterface?, _: Int ->
                startActivity(
                    Intent(
                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )
            }
            .setNegativeButton(getString(R.string.no)) { dialog: DialogInterface, _: Int -> dialog.cancel() }
            .create()
    }

    companion object {
        private const val TAG = "LocationToggleDialogFragment"

        fun show(manager: FragmentManager) {
            val dialogFragment = LocationToggleDialogFragment()
            dialogFragment.show(manager, TAG)
        }
    }
}