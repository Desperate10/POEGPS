package com.poe.poegps.feature.presentation.screens.main

import androidx.lifecycle.viewModelScope
import com.poe.poegps.feature.data.remote.model.LoginResponse
import com.poe.poegps.feature.presentation.model.ObjectState
import com.poe.poegps.feature.presentation.model.ObjectType
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.data.remote.utils.FilialManager
import com.poe.poegps.feature.data.remote.utils.TokenManager
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import com.poe.poegps.feature.presentation.BaseViewModel
import com.poe.poegps.feature.presentation.CoroutinesErrorHandler
import com.poe.poegps.feature.presentation.mapper.toObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ObjectsRepository,
    private val tokenManager: TokenManager,
    private val filialManager: FilialManager
) : BaseViewModel() {

    val token = MutableStateFlow<String?>("")
    val filial = MutableStateFlow<String?>("")

    private val _spinnerObjectType = MutableStateFlow(ObjectType.LINE)
    val spinnerObjectType = _spinnerObjectType

    private val _objectsList = MutableStateFlow<List<ObjectDisplayable>>(emptyList())
    val objectsList = _objectsList

    private val _loginResponse = MutableStateFlow<ApiResponse<LoginResponse>>(ApiResponse.Loading)
    val loginResponse = _loginResponse

    private val _state = MutableStateFlow<ObjectState>(ObjectState.initial)
    val state: StateFlow<ObjectState> = _state

    init {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.getToken().collect {
                withContext(Dispatchers.Main) {
                    token.value = it
                }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            filialManager.getFilial().collect {
                withContext(Dispatchers.Main) {
                    if (it == null) filial.value = "00"
                    else filial.value = it
                }
            }
        }
    }

    fun loadObjectsToDb() {
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            object : CoroutinesErrorHandler {
                override fun onError(message: String) {
                    error.localizedMessage ?: "Error occured! Please try again."
                }
            }
        }) {
            repository.downloadLines(20, token.value ?: "")
        }
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            object : CoroutinesErrorHandler {
                override fun onError(message: String) {
                    error.localizedMessage ?: "Error occured! Please try again."
                }
            }
        }) {
            repository.downloadTPs(20, token.value ?: "")
        }
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            object : CoroutinesErrorHandler {
                override fun onError(message: String) {
                    error.localizedMessage ?: "Error occured! Please try again."
                }
            }
        }) {
            repository.downloadPss(token.value ?: "")
        }
        viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
            object : CoroutinesErrorHandler {
                override fun onError(message: String) {
                    error.localizedMessage ?: "Error occured! Please try again."
                }
            }
        }) {
            repository.downloadPillars(20, token.value ?: "")
        }
    }

    fun authorization(
        login: String,
        password: String,
        coroutineErrorHandler: CoroutinesErrorHandler
    ) = baseRequest(
        _loginResponse,
        coroutineErrorHandler
    ) {
        repository.auth(login, password)
    }

    fun saveToken(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.saveToken(token)
        }
    }

    fun deleteToken() {
        viewModelScope.launch(Dispatchers.IO) {
            tokenManager.deleteToken()
        }
    }

    fun searchObject(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _objectsList,
        coroutineErrorHandler
    ) {
        // return@baseRequest when(spinnerObjectType.value) {
        //     ObjectType.LINE ->
        repository.searchLine(tplnr).map { it.map { it.toObjectDisplayable() } }
        //     ObjectType.TP -> repository.searchTP(tplnr).map { it.map { it.toObjectDisplayable() } }
        // }
    }

    fun selectedObjectType(type: String) {
        when (type) {
            "Линия" -> _spinnerObjectType.value = ObjectType.LINE
            "ТП" -> _spinnerObjectType.value = ObjectType.TP
        }
    }

    fun selectedFilial(filial: String) {
        viewModelScope.launch(Dispatchers.IO) {
            filialManager.saveFilial(filial)
        }
    }

}