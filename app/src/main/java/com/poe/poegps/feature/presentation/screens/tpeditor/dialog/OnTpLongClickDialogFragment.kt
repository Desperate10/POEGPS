package com.poe.poegps.feature.presentation.screens.tpeditor.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.screens.editor.EditorViewModel
import com.poe.poegps.feature.presentation.screens.editor.dialog.ChoosePillarFrom10DialogFragment
import com.poe.poegps.feature.presentation.screens.editor.dialog.OnPillarLongClickDialogFragment

class OnTpLongClickDialogFragment: DialogFragment() {

    private lateinit var tplnr: String

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TPLNR, tplnr)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        tplnr = if (arguments == null) {
            savedInstanceState?.getString(TPLNR).toString()
        } else {
            requireArguments().getString(TPLNR).toString()
        }

        val options = arrayOf(
            getString(R.string.clear_tp_coord),
            getString(R.string.cancel)
        )

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.choose_action)
            .setItems(options) { _: DialogInterface, item: Int ->
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        TPLNR to tplnr,
                        KEY_BUTTON to options[item])
                )
            }
            .setCancelable(true)
            .create()
    }

    companion object {
        private const val TAG = "OnTpLongClickDialogFragment"
        private const val TPLNR = "tplnr"
        const val REQUEST_KEY = "$TAG:clearOrNot"
        const val KEY_BUTTON = "button"

        fun show(fragmentManager: FragmentManager, tplnr: String) {
            val dialogFragment = OnTpLongClickDialogFragment()
            dialogFragment.arguments = bundleOf(TPLNR to tplnr)
            dialogFragment.show(fragmentManager, TAG)
        }

        fun setupListeners(
            manager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            listener: (String, String) -> Unit
        ) {
            manager.setFragmentResultListener(
                REQUEST_KEY,
                lifecycleOwner
            ) { _, bundle ->
                val tplnr = bundle.getString(TPLNR)
                val button = bundle.getString(KEY_BUTTON)
                listener(tplnr!!, button!!)
            }
        }
    }
}