package com.poe.poegps.feature.presentation.screens.main

import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.poe.poegps.R
import com.poe.poegps.databinding.FragmentMainBinding
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.presentation.CoroutinesErrorHandler
import com.poe.poegps.feature.presentation.screens.main.dialog.LoginDialogFragment
import com.poe.poegps.feature.presentation.screens.main.dialog.SearchObjectDialog
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.screens.main.adapter.ObjectsAdapter
import com.poe.poegps.feature.presentation.screens.main.spinner.ObjectsSpinnerAdapter
import dagger.hilt.android.AndroidEntryPoint
import gromov.ramdomusertestcase.core.extension.autoCleaned
import gromov.ramdomusertestcase.core.extension.collectLifecycleFlow

@AndroidEntryPoint
class MainFragment : Fragment(), ObjectsAdapter.OnObjectClickListener, MenuProvider,
    SearchObjectDialog.OnInputListener,
    AdapterView.OnItemSelectedListener {

    private var binding: FragmentMainBinding by autoCleaned()
    private var adapter: ObjectsAdapter by autoCleaned()
    private val viewModel by viewModels<MainViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObjectSpinner()
        setupObjectsAdapter()
        collectViewModel()
        requireActivity().addMenuProvider(this)
    }

    private fun setupObjectsAdapter() {
        val linearLayoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        adapter = ObjectsAdapter()
        binding.objectsRv.adapter = adapter
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.objectsRv.layoutManager = linearLayoutManager
        adapter.onObjectClickListener = this
    }

    private fun collectViewModel() {
        collectLifecycleFlow(viewModel.token) { token ->
            if (token.isNotEmpty()) {
                Log.d("testim", "token: $token")
            } else {
                //LoginDialogFragment().show(childFragmentManager, "login")
                viewModel.authorization("poegis",
                    "123Qwerty",
                    object : CoroutinesErrorHandler {
                        override fun onError(message: String) {
                            Toast.makeText(context, "Error! $message", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
        }
        collectLifecycleFlow(viewModel.loginResponse) { loginResponse ->
            when (loginResponse) {
                is ApiResponse.Error -> Toast.makeText(
                    requireContext(),
                    loginResponse.message,
                    Toast.LENGTH_SHORT
                ).show()

                ApiResponse.Loading -> Toast.makeText(
                    requireContext(),
                    "Loading...",
                    Toast.LENGTH_SHORT
                ).show()

                is ApiResponse.Success -> {
                    viewModel.saveToken(loginResponse.data.jwt)
                }
            }
        }
        collectLifecycleFlow(viewModel.objectsList) { objectsList ->
            Log.d("testim", "objectsList: $objectsList")
            adapter.submitList(objectsList)
        }
        collectLifecycleFlow(viewModel.filial) { filial ->
            if (filial == "00") {
                chooseYourFilialDialog()
            } else {
                Log.d("testim", "filial: $filial")
            }
        }
    }

    private fun setupObjectSpinner() {
        binding.objectSpinner.adapter = ObjectsSpinnerAdapter(requireContext())
    }

    override fun onClick(obj: ObjectDisplayable) {
        navigateToEditObjectFragment(obj)
    }

    override fun onLongClick(obj: ObjectDisplayable) {
        val popupMenu = PopupMenu(requireContext(), binding.objectsRv)
        popupMenu.inflate(R.menu.menu_context_main)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.deleteOtp -> {
                    true
                }

                R.id.sendObject -> {
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.search)
        val searchView = searchItem?.actionView as SearchView
        searchView.imeOptions = EditorInfo.IME_ACTION_DONE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(tplnr: String?): Boolean {
                tplnr?.let {
                    viewModel.searchObject(tplnr, object : CoroutinesErrorHandler {
                        override fun onError(message: String) {
                            Toast.makeText(requireContext(), "Error! $message", Toast.LENGTH_SHORT)
                                .show()
                        }
                    })
                }
                return false
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                //adapter.getFilter().filter(newText);
                return false
            }
        })
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.search -> {
                val searchDialog =
                    SearchObjectDialog().newInstance(objects = ArrayList(), "Виберіть об'єкт:")
                searchDialog.show(childFragmentManager, "searchDialog")
                true
            }

            R.id.download -> {
                viewModel.loadObjectsToDb()
                true
            }

            else -> false
        }
    }

    //пока не используется
    override fun sendInput(input: String?) {
        Toast.makeText(requireContext(), input, Toast.LENGTH_SHORT).show()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        when (parent?.id) {
            R.id.object_spinner -> {
                val item = parent.getItemAtPosition(position) as String
                viewModel.selectedObjectType(item)
            }
        }
    }

    private fun navigateToEditObjectFragment(obj: ObjectDisplayable) {
        findNavController().navigate(
            MainFragmentDirections.actionMainFragmentToEditorFragment(
                obj.tplnr,
                obj.name,
            )
        )
    }

    private fun chooseYourFilialDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_filial, null)
        builder.setCancelable(false)
        builder.setView(dialogView)
        val filials = resources.getStringArray(R.array.filials)
        val spinner = dialogView.findViewById<View>(R.id.spinner) as Spinner
        // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, filials)
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        // Применяем адаптер к элементу spinner
        spinner.adapter = adapter
        adapter.notifyDataSetChanged()
        val alertDialog = builder.create()
        alertDialog.show()
        val create = dialogView.findViewById<Button>(R.id.choose)
        create.setOnClickListener { view: View? ->
            val filial = spinner.selectedItem.toString().take(2)
            viewModel.selectedFilial(filial)
            alertDialog.cancel()
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().removeMenuProvider(this)
    }

}