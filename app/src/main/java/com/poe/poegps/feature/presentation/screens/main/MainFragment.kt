package com.poe.poegps.feature.presentation.screens.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.Toast
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
import com.poe.poegps.feature.presentation.dialog.SearchObjectDialog
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
        viewModel.authorization("poegis",
            "123Qwerty",
            object : CoroutinesErrorHandler {
                override fun onError(message: String) {
                    Toast.makeText(context, "Error! $message", Toast.LENGTH_SHORT).show()
                }
            })
        //val loginDialog = LoginDialogFragment()
        //loginDialog.show(childFragmentManager, "loginDialog")
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
            adapter.submitList(objectsList)
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
                //Log.d("testim", "onQueryTextSubmit: $tplnr")
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

    override fun onNothingSelected(parent: AdapterView<*>?) {}


}