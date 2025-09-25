package com.poe.poegps.feature.presentation.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poe.poegps.feature.domain.model.Recloser
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import com.poe.poegps.feature.presentation.mapper.toDomainModel
import com.poe.poegps.feature.presentation.mapper.toLineDomainModel
import com.poe.poegps.feature.presentation.mapper.toObjectDisplayable
import com.poe.poegps.feature.presentation.mapper.toOprDisplayable
import com.poe.poegps.feature.presentation.model.ObjectDisplayable
import com.poe.poegps.feature.presentation.model.ObjectType
import com.poe.poegps.feature.presentation.model.OprDisplayable
import com.poe.poegps.feature.presentation.model.RecloserDisplayable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class InsertResult {
    object Success : InsertResult()
    object Duplicate : InsertResult()
    object Failure : InsertResult() // For any other unexpected error
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: ObjectsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args by lazy { EditorFragmentArgs.fromSavedStateHandle(savedStateHandle) }

    private val _lineName = MutableStateFlow<String>(args.pltxt)
    val lineName = _lineName

    private val _oprDisplayable = MutableStateFlow<List<OprDisplayable>>(emptyList())
    val oprDisplayable = _oprDisplayable

    private val _parentObjectName = MutableStateFlow<String>("")
    val parentObjectName = _parentObjectName

    // Channel to send one-time UI events
    private val _insertResultChannel = Channel<InsertResult>()
    val insertResultFlow = _insertResultChannel.receiveAsFlow() // Expose as a Flow for collection


    init {
        viewModelScope.launch {
            _lineName.flatMapLatest {
                repository.getSavedPillars(it)
            }.collectLatest { list ->
                _oprDisplayable.value = list.map { opr -> opr.toOprDisplayable() }
            }
        }
    }

    fun setLineName(lineName: String) {
        _lineName.value = lineName
    }

    fun getPillar04List(tplnr: String) =
        repository.getPillar04List(tplnr).map { list ->
            list.map { opr -> opr.toOprDisplayable() }
        }

    fun getPillar10List(tplnr: String) =
        repository.getPillar10List(tplnr).map { list ->
            list.map { opr -> opr.toOprDisplayable() }
        }

    fun getLineList10() =
        repository.getLineList10().map { list ->
            list.map { line -> line.toObjectDisplayable() }
        }

    fun getLineList04() =
        repository.getLineList04().map { list ->
            list.map { line -> line.toObjectDisplayable() }
        }

    fun getTpList(isAbon: Boolean) =
        repository.getTpList(isAbon).map { list ->
            list.map { tp -> tp.toOprDisplayable() }
        }

    fun addPillarToDisplay(obj: OprDisplayable) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rowId = repository.savePillar(obj.toDomainModel())
                if (rowId != -1L) {
                    _insertResultChannel.send(InsertResult.Success)
                } else {
                    // Room returns -1L when INSERT ... WHERE NOT EXISTS prevents insertion
                    _insertResultChannel.send(InsertResult.Duplicate)
                }
            } catch (e: Exception) {
                // Catch any other potential database errors during insertion
                _insertResultChannel.send(InsertResult.Failure)
                // You might also log the error: Log.e("PillarViewModel", "Error saving pillar", e)
            }
        }
    }

    fun addRecloserToPillar(id: Int, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRecloserToPillar(id, name)
        }
    }

    fun createRecloser(recloser: RecloserDisplayable) {
        viewModelScope.launch {
            repository.saveRecloser(recloser.toDomainModel())
        }
    }

    fun createOtp(pillarStart: OprDisplayable, pillarNext: OprDisplayable) {
        viewModelScope.launch {
            repository.copyPillarForOtp(pillarStart.copy(id = 0).toDomainModel())
            repository.savePillar(pillarNext.toDomainModel())
            when(args.objectType) {
                ObjectType.LINE04 -> repository.saveLine04(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                ObjectType.LINE10 -> repository.saveLine10(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                ObjectType.LINEABON04 -> repository.saveAbonLine04(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                ObjectType.LINEABON10 -> repository.saveAbonLine10(
                    ObjectDisplayable(
                        tplnr = pillarStart.tplnr,
                        name = pillarStart.parentName,
                        category = pillarStart.category,
                        pillarType = pillarStart.pillarType,
                        isAbon = pillarStart.isAbon
                    ).toLineDomainModel()
                )
                /* ObjectType.LINE35 -> TODO()
                 ObjectType.LINE110 -> TODO()
                 ObjectType.LINE154 -> TODO()
                 ObjectType.LINEABON35 -> TODO()
                 ObjectType.LINEABON110 -> TODO()
                 ObjectType.LINEABON154 -> TODO()*/
                ObjectType.TP -> {
                }
                ObjectType.TPABON -> {}
                ObjectType.KLKV04 -> {}
                ObjectType.KLKV10 -> {}
                ObjectType.KLKVABON04 -> {}
                ObjectType.KLKVABON10 -> {}
            }
            /*repository.saveLine(
                ObjectDisplayable(
                    tplnr = pillarStart.tplnr,
                    name = pillarStart.parentName
                ).toLineDomainModel()
            )*/
        }
    }

    fun clearCoord(pillar: OprDisplayable?) {
       // if (pillar?.order != 1) {
            val pillarNew = pillar?.copy(lat = "0.0", lng = "0.0")
            viewModelScope.launch(Dispatchers.IO) {
                pillarNew?.toDomainModel()?.let { repository.savePillar(it) }
            }
       // }
    }

    fun deleteOpr(pillar: OprDisplayable?) {
        viewModelScope.launch(Dispatchers.IO) {
            pillar?.toDomainModel()?.let { repository.deletePillar(it) }
        }
    }

    fun getParentName(tplnr: String) {
        viewModelScope.launch {
            _parentObjectName.value = repository.getParentName(tplnr)
        }
    }

    fun saveCoordinates(opr: OprDisplayable) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePillar(opr.toDomainModel())
        }
    }

    fun getWires(category: String) = repository.getWireList(category)

    /*fun getLineList10() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.getLineList10().collectLatest {
                _lineList10.emit(it.map { opr -> opr.toObjectDisplayable() })
            }
        }
    }*/

    //функция получения списка ТП или ПС по конкретной категории напряжения(10 или 0.4)
    //fun getTpList(category: String) = repository.getTpList(category)


}