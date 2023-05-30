package com.poe.poegps.feature.presentation.screens.main

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.poe.poegps.feature.data.remote.model.LoginResponse
import com.poe.poegps.feature.data.remote.model.SavePillarsResponse
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.data.remote.utils.FilialManager
import com.poe.poegps.feature.data.remote.utils.TokenManager
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import com.poe.poegps.feature.presentation.BaseViewModel
import com.poe.poegps.feature.presentation.CoroutinesErrorHandler
import com.poe.poegps.feature.presentation.mapper.toObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectState
import com.poe.poegps.feature.presentation.model.ObjectType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
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

    private val _spinnerObjectType = MutableStateFlow(ObjectType.LINE04)
    val spinnerObjectType = _spinnerObjectType

    private val _objectsList = MutableStateFlow<List<ObjectDisplayable>>(emptyList())
    val objectsList = _objectsList

    private val _loginResponse = MutableStateFlow<ApiResponse<LoginResponse>>(ApiResponse.Loading)
    val loginResponse = _loginResponse.stateIn(viewModelScope, SharingStarted.Lazily, ApiResponse.Loading)

    private val _uploadResponse = MutableStateFlow<SavePillarsResponse>(SavePillarsResponse(false, ""))
    val uploadResponse = _uploadResponse.stateIn(viewModelScope, SharingStarted.Lazily, SavePillarsResponse(false, ""))

    private val _state = MutableStateFlow<ObjectState>(ObjectState.initial)
    val state: StateFlow<ObjectState> = _state

    private val _tokenValidity = MutableStateFlow<Boolean>(true)
    val tokenValidity = _tokenValidity

    private val _message = MutableSharedFlow<String>()
    val message = _message

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

    /*fun checkTokenValidity(
        coroutineErrorHandler: CoroutinesErrorHandler
    ) = viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
        repository.checkTokenValidity(token.value ?: "")
    }) {
        _tokenValidity.value = repository.checkTokenValidity(token.value ?: "")
    }*/

    fun loadObjectsToDb() {
        if (filial.value != null) {
            viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
                object : CoroutinesErrorHandler {
                    override fun onError(message: String) {
                        error.localizedMessage ?: "Error occured! Please try again."
                    }
                }
            }) {
                repository.downloadLines(filial.value!!, token.value ?: "")
            }
            viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
                object : CoroutinesErrorHandler {
                    override fun onError(message: String) {
                        error.localizedMessage ?: "Error occured! Please try again."
                    }
                }
            }) {
                repository.downloadTPs(filial.value!!, token.value ?: "")
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
                repository.downloadPillars(filial.value!!, token.value ?: "")
            }
        } else {
            viewModelScope.launch(Dispatchers.Main) {
                _message.emit("Не выбрано філіал!")
            }
        }
    }

    fun authorization(
        login: String, password: String, coroutineErrorHandler: CoroutinesErrorHandler
    ) = baseRequest(
        _loginResponse, coroutineErrorHandler
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

    /*fun searchedObjects(tplnr: String) {
        _objectsList.value = _spinnerObjectType
        .flatMapLatest {
            when (it) {
                ObjectType.LINE -> {
                    Log.d("zashlo", "line")
                    _abonState.flatMapLatest { abonState ->
                        repository.searchLine(tplnr, abonState).map {
                            it.map { it.toObjectDisplayable() }
                        }
                    }
                }
                ObjectType.TP -> {
                    Log.d("zashlo", "tp")
                    _abonState.flatMapLatest { abonState ->
                        repository.searchTP(tplnr, abonState).map {
                            it.map { it.toObjectDisplayable() }
                        }
                    }
                }
            }
        }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
            .value
    }*/

    fun searchLine04(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _objectsList, coroutineErrorHandler
    ) {
        repository.searchLine04(tplnr).map { it.map { it.toObjectDisplayable() } }
    }

    fun searchLine10(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _objectsList, coroutineErrorHandler
    ) {
        repository.searchLine10(tplnr).map { it.map { it.toObjectDisplayable() } }
    }

    fun searchLine(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) =
        baseRequest(
            _objectsList, coroutineErrorHandler
        ) {
            if (_spinnerObjectType.value == ObjectType.LINE04) {
                repository.searchLine04(tplnr).map { it.map { it.toObjectDisplayable() } }
            } else {
                repository.searchAbonLine04(tplnr).map { it.map { it.toObjectDisplayable() } }
            }
        }

    fun searchAbonLine10(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) =
        baseRequest(
            _objectsList, coroutineErrorHandler
        ) {
            repository.searchLine10(tplnr).map { it.map { it.toObjectDisplayable() } }
        }

    fun searchTp(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _objectsList,
        coroutineErrorHandler
    ) {
        if (_spinnerObjectType.value == ObjectType.TP) {
            repository.searchTP(tplnr, false).map { it.map { it.toObjectDisplayable() } }
        } else {
            repository.searchTP(tplnr, true).map { it.map { it.toObjectDisplayable() } }
        }
    }

    fun searchPs(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _objectsList, coroutineErrorHandler
    ) {
        repository.searchPS(tplnr).map { it.map { it.toObjectDisplayable() } }
    }

    fun deleteObject(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (_spinnerObjectType.value) {
                ObjectType.LINE04 -> repository.deleteLine04(name)
                ObjectType.LINE10 -> repository.deleteLine10(name)
                ObjectType.LINEABON04 -> repository.deleteAbonLine04(name)
                ObjectType.LINEABON10 -> repository.deleteAbonLine10(name)
                /*ObjectType.LINE35 -> TODO()
                ObjectType.LINE110 -> TODO()
                ObjectType.LINE154 -> TODO()
                ObjectType.LINEABON35 -> TODO()
                ObjectType.LINEABON110 -> TODO()
                ObjectType.LINEABON154 -> TODO()*/
                ObjectType.TP -> TODO()
                ObjectType.TPABON -> TODO()
                ObjectType.PS -> TODO()
            }
        }
    }

    /*fun searchTp(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _objectsList,
        coroutineErrorHandler
    ) {
        repository.searchLine10(tplnr, false).map { it.map { it.toObjectDisplayable() } }
        //repository.searchTP(tplnr, false).map { it.map { it.toObjectDisplayable() } }
    }

    fun searchObject(tplnr: String, coroutineErrorHandler: CoroutinesErrorHandler) = baseRequest(
        _objectsList,
        coroutineErrorHandler
    ) {

         //return@baseRequest when(spinnerObjectType.value) {
          //   ObjectType.LINE ->
                //    repository.searchLine(tplnr, false).map { it.map { it.toObjectDisplayable() } }
           //  ObjectType.TP ->
       // repository.searchTP(tplnr).map { it.map { it.toObjectDisplayable() } }
         //}
    }*/

    fun selectedObjectType(objectType: ObjectType) {
        _spinnerObjectType.value = objectType
    }

    fun selectedFilial(filial: String) {
        viewModelScope.launch(Dispatchers.IO) {
            filialManager.saveFilial(filial)
        }
    }

    fun uploadObjectsFromDb(
        coroutineErrorHandler: CoroutinesErrorHandler
    ) = baseRequest(
        _uploadResponse, coroutineErrorHandler
    ) {
        repository.uploadSavedPillars(token.value ?: "")
    }

    fun resetResponse() {
        _uploadResponse.value = SavePillarsResponse(false, "")
    }

}