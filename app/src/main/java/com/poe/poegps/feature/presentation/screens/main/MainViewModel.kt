package com.poe.poegps.feature.presentation.screens.main

import androidx.lifecycle.viewModelScope
import com.poe.poegps.feature.data.remote.model.LoginResponse
import com.poe.poegps.feature.data.remote.model.TokenCheckResponse
import com.poe.poegps.feature.data.remote.model.upload.UploadState
import com.poe.poegps.feature.data.remote.utils.ApiResponse
import com.poe.poegps.feature.data.remote.utils.FilialManager
import com.poe.poegps.feature.data.remote.utils.TokenManager
import com.poe.poegps.feature.domain.model.LineProgress
import com.poe.poegps.feature.domain.model.UploadGroup
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import com.poe.poegps.feature.presentation.BaseViewModel
import com.poe.poegps.feature.presentation.CoroutinesErrorHandler
import com.poe.poegps.feature.presentation.mapper.toObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** One card in the objects list: the object and its capture progress, if work on it started. */
data class ObjectItem(
    val obj: ObjectDisplayable,
    val progress: LineProgress?,
)

data class MainUiState(
    val objectType: ObjectType = ObjectType.LINE04,
    val query: String = "",
    val items: List<ObjectItem> = emptyList(),
    val total: Int = 0,
    val unsent: Int = 0,
    val filial: String? = null,
    val download: UploadState = UploadState.Idle,
)

sealed interface SyncUiState {
    object Uploading : SyncUiState
    data class Done(
        val accepted: Int,
        val total: Int,
        val rows: List<SyncRow>,
        val time: String,
        val rejectReason: String,
    ) : SyncUiState
    data class Failed(val message: String) : SyncUiState
}

data class SyncRow(
    val name: String,
    val poles: Int,
    val reclosers: Int,
    val rejected: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ObjectsRepository,
    private val tokenManager: TokenManager,
    private val filialManager: FilialManager
) : BaseViewModel() {

    private val token = MutableStateFlow<String?>("")
    val filial = MutableStateFlow<String?>("")

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _spinnerObjectType = MutableStateFlow(ObjectType.LINE04)
    val spinnerObjectType: StateFlow<ObjectType> = _spinnerObjectType

    private val query = MutableStateFlow("")

    private val _loginResponse = MutableStateFlow<ApiResponse<LoginResponse>>(ApiResponse.Loading)
    val loginResponse: StateFlow<ApiResponse<LoginResponse>> = _loginResponse

    private val _tokenValidity =
        MutableStateFlow<ApiResponse<TokenCheckResponse>>(ApiResponse.Loading)
    val tokenValidity: StateFlow<ApiResponse<TokenCheckResponse>> = _tokenValidity

    private var lastUploadTplnr = ""
    private val _sync = MutableStateFlow<SyncUiState?>(null)
    val sync: StateFlow<SyncUiState?> = _sync

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message

    private val objects: Flow<List<ObjectDisplayable>> =
        combine(_spinnerObjectType, query.debounce(200)) { type, q -> type to q.trim() }
            .flatMapLatest { (type, q) -> search(type, q) }

    private val total: Flow<Int> =
        _spinnerObjectType.flatMapLatest { search(it, "") }.map { it.size }

    private val items: Flow<List<ObjectItem>> =
        combine(objects, repository.getLineProgress()) { list, progress ->
            val byLine = progress.associateBy { it.tplnr to it.parentName }
            // a TP's coordinates are one line_pillar row keyed by its tplnr
            val byTplnr = progress.groupBy { it.tplnr }.mapValues { (_, rows) ->
                rows.reduce { a, b -> a.copy(total = a.total + b.total, captured = a.captured + b.captured, unsent = a.unsent + b.unsent) }
            }
            val isTp = _spinnerObjectType.value == ObjectType.TP || _spinnerObjectType.value == ObjectType.TPABON
            list.map { obj ->
                ObjectItem(obj, if (isTp) byTplnr[obj.tplnr] else byLine[obj.tplnr to obj.name])
            }
        }

    val uiState: StateFlow<MainUiState> = combine(
        combine(_spinnerObjectType, query, items, total) { type, q, list, count ->
            MainUiState(objectType = type, query = q, items = list, total = count)
        },
        repository.countUnsent(),
        filial,
        _uploadState,
    ) { state, unsent, filialCode, download ->
        state.copy(unsent = unsent, filial = filialCode, download = download)
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun initialize() {
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
        viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            tokenCheck(object : CoroutinesErrorHandler {
                override fun onError(message: String) {
                    viewModelScope.launch(Dispatchers.Main) {
                        _message.emit("Не вдалося перевірити токен! Перевірте підключення до інтернету та спробуйте ще раз.")
                    }
                }
            })
        }
    }

    private fun search(type: ObjectType, q: String): Flow<List<ObjectDisplayable>> = when (type) {
        ObjectType.LINE04 -> repository.searchLine04(q).map { it.map { l -> l.toObjectDisplayable() } }
        ObjectType.LINE10 -> repository.searchLine10(q).map { it.map { l -> l.toObjectDisplayable() } }
        ObjectType.LINEABON04 -> repository.searchAbonLine04(q).map { it.map { l -> l.toObjectDisplayable() } }
        ObjectType.LINEABON10 -> repository.searchAbonLine10(q).map { it.map { l -> l.toObjectDisplayable() } }
        ObjectType.TP -> repository.searchTP(q).map { it.map { t -> t.toObjectDisplayable() } }
        ObjectType.TPABON -> repository.searchAbonTP(q).map { it.map { t -> t.toObjectDisplayable() } }
        ObjectType.KLKV04 -> repository.searchKl04(q).map { it.map { l -> l.toObjectDisplayable() } }
        ObjectType.KLKV10 -> repository.searchKl10(q).map { it.map { l -> l.toObjectDisplayable() } }
        ObjectType.KLKVABON04 -> repository.searchAbonKl04(q).map { it.map { l -> l.toObjectDisplayable() } }
        ObjectType.KLKVABON10 -> repository.searchAbonKl10(q).map { it.map { l -> l.toObjectDisplayable() } }
    }

    fun setQuery(text: String) {
        query.value = text
    }

    fun selectedObjectType(objectType: ObjectType) {
        _spinnerObjectType.value = objectType
    }

    fun loadObjectsToDb() {
        if (filial.value != null) {
            val jobs = mutableListOf<Job>()
            jobs += viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, error ->
                _uploadState.value = UploadState.Error(error.localizedMessage ?: "Помилка завантаження даних")
            }) {
                repository.downloadLines(filial.value!!, token.value ?: "")
                    .collect {
                        _uploadState.value = it
                    }
            }
            jobs += viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }) {
                repository.downloadTPs(filial.value!!, token.value ?: "")
            }
            jobs += viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }) {
                repository.downloadPss(token.value ?: "")
            }
            jobs += viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }) {
                repository.downloadPillars(filial.value!!, token.value ?: "")
            }
            //загрузка проводов
            jobs += viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }) {
                repository.downloadWires(token.value ?: "")
            }
            //загрузка КЛ/КВ
            jobs += viewModelScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, _ -> }) {
                repository.downloadKls(filial.value!!, token.value ?: "")
            }
            // B12 shows when the branch data was last downloaded in full
            viewModelScope.launch {
                jobs.joinAll()
                if (jobs.none { it.isCancelled } && _uploadState.value !is UploadState.Error) {
                    filialManager.saveDownloadedAt(System.currentTimeMillis())
                }
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

    private fun tokenCheck(
        coroutineErrorHandler: CoroutinesErrorHandler
    ) = baseRequest(
        _tokenValidity, coroutineErrorHandler
    ) {
        repository.tokenCheck(token.value ?: "")
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

    fun deleteObject(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (_spinnerObjectType.value) {
                ObjectType.LINE04 -> repository.deleteLine04(name)
                ObjectType.LINE10 -> repository.deleteLine10(name)
                ObjectType.LINEABON04 -> repository.deleteAbonLine04(name)
                ObjectType.LINEABON10 -> repository.deleteAbonLine10(name)
                ObjectType.TP -> {}
                ObjectType.TPABON -> {}
                ObjectType.KLKV04 -> {}
                ObjectType.KLKV10 -> {}
                ObjectType.KLKVABON04 -> {}
                ObjectType.KLKVABON10 -> {}
            }
        }
    }

    fun selectedFilial(filial: String) {
        viewModelScope.launch(Dispatchers.IO) {
            filialManager.saveFilial(filial)
        }
    }

    /** Uploads everything unsent, or one object when [tplnr] is given, and reports the result for B5. */
    fun upload(tplnr: String = "") {
        if (_sync.value == SyncUiState.Uploading) return
        lastUploadTplnr = tplnr
        _sync.value = SyncUiState.Uploading
        viewModelScope.launch(Dispatchers.IO) {
            _sync.value = try {
                val (groups, recloserCounts) = repository.getUploadGroups(tplnr)
                val reclosers = if (tplnr.isEmpty()) recloserCounts else recloserCounts.filterKeys { it.contains(tplnr) }
                if (groups.isEmpty() && reclosers.isEmpty()) {
                    _message.emit("Немає даних для вивантаження")
                    null
                } else {
                    // the flow emits nothing when the server answers without a body (HTTP error)
                    val response = repository.uploadSavedPillars(token.value ?: "", tplnr).firstOrNull()
                    when {
                        response == null -> SyncUiState.Failed("Сервер не прийняв запит. Можливо, потрібно увійти знову.")
                        response.error -> SyncUiState.Failed(response.message.ifEmpty { "Сервер повернув помилку" })
                        else -> doneState(groups, reclosers, response.tplnrList.toSet(), response.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: java.io.IOException) {
                SyncUiState.Failed("Немає зв'язку з сервером ГІС. Перевірте інтернет і спробуйте ще раз.")
            } catch (e: Exception) {
                SyncUiState.Failed(e.localizedMessage ?: "Невідома помилка")
            }
        }
    }

    private fun doneState(
        groups: List<UploadGroup>,
        reclosers: Map<String, Int>,
        rejected: Set<String>,
        message: String,
    ): SyncUiState.Done {
        // reclosers are reported on the first row of their line, or on a row of their own
        val unclaimed = reclosers.toMutableMap()
        val rows = groups.map { g ->
            SyncRow(g.name, g.poles, unclaimed.remove(g.tplnr) ?: 0, g.tplnr in rejected)
        } + unclaimed.map { (tplnr, count) -> SyncRow(tplnr, 0, count, tplnr in rejected) }
        val total = rows.sumOf { it.poles + it.reclosers }
        val accepted = rows.filterNot { it.rejected }.sumOf { it.poles + it.reclosers }
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        return SyncUiState.Done(accepted, total, rows, time, message)
    }

    fun retryUpload() {
        _sync.value = null
        upload(lastUploadTplnr)
    }

    fun closeSync() {
        if (_sync.value != SyncUiState.Uploading) _sync.value = null
    }

    fun setLoginResponseToLoading() {
        _loginResponse.value = ApiResponse.Loading
    }

    fun setUploadStateToIdle() {
        _uploadState.value = UploadState.Idle
    }

    fun setTokenValidity() {
        _tokenValidity.value = ApiResponse.Success(data = TokenCheckResponse(error = false, message = ""))
    }
}
