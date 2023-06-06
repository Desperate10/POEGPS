package com.poe.poegps.feature.presentation.screens.main.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.poe.poegps.R
import com.poe.poegps.feature.presentation.CoroutinesErrorHandler
import com.poe.poegps.feature.presentation.screens.main.MainViewModel

class AuthorizationDialogFragment: DialogFragment() {

    private val viewModel by activityViewModels<MainViewModel>()

    var login: EditText? = null
    var password: EditText? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.login_layout, null)
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.authorization)
        builder.setView(view)

        login = view.findViewById<EditText>(R.id.login)
        password  = view.findViewById<EditText>(R.id.password)

        builder.setPositiveButton("Вхід", null)


        return builder.create()
    }

    override fun onResume() {
        super.onResume()
        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positiveButton.setOnClickListener {
            if (login?.text.toString().isNotEmpty() && password?.text.toString().isNotEmpty()) {
                viewModel.authorization(
                    login!!.text.toString(),
                    password!!.text.toString(),
                    object : CoroutinesErrorHandler {
                        override fun onError(message: String) {
                            Toast.makeText(context, "Error! $message", Toast.LENGTH_SHORT).show()
                        }
                    })
                // Toast.makeText(getActivity(), "Успешно "+response.body().jwt, Toast.LENGTH_LONG).show();
                dialog.dismiss()
            } else {
                Toast.makeText(context, "Заповніть всі поля", Toast.LENGTH_SHORT).show()
            }
        }
    }
}