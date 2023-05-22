package com.poe.poegps.feature.presentation.screens.editor.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.model.OprDisplayable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnPillarLongClickDialogFragment: DialogFragment() {

    private lateinit var pillar: OprDisplayable

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(PILLAR, pillar)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        pillar = if (arguments == null) {
            savedInstanceState?.getParcelable(PILLAR)!!
        } else {
            requireArguments().getParcelable(PILLAR)!!
        }

        val options = arrayOf(
            getString(R.string.create_otp),
            getString(R.string.clear_coord),
            getString(R.string.delete_opr),
            getString(R.string.cancel)
        )

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.choose_action)
            .setItems(options) { _: DialogInterface, item: Int ->
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(
                        PILLAR to pillar,
                        KEY_BUTTON to options[item].toString())
                )
            }
            .setCancelable(true)
            .create()
    }

    companion object {
        private const val TAG = "OnPillarLongClickDialogFragment"
        private const val PILLAR = "pillar"
        const val REQUEST_KEY = "$TAG:clearOrNot"
        const val KEY_BUTTON = "button"

        fun show(fragmentManager: FragmentManager, pillar: OprDisplayable) {
            val dialogFragment = OnPillarLongClickDialogFragment()
            dialogFragment.arguments = bundleOf(PILLAR to pillar)
            dialogFragment.show(fragmentManager, TAG)
        }

        fun setupListeners(
            manager: FragmentManager,
            lifecycleOwner: LifecycleOwner,
            listener: (OprDisplayable, String) -> Unit
        ) {
            manager.setFragmentResultListener(
                REQUEST_KEY,
                lifecycleOwner
            ) { _, bundle ->
                val pillar = bundle.getParcelable<OprDisplayable>(PILLAR)
                val button = bundle.getString(KEY_BUTTON)
                listener(pillar!!, button!!)
            }
        }
    }
}