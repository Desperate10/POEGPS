package com.poe.poegps.feature.presentation.screens.tpeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import com.poe.poegps.feature.presentation.mapper.toDomainModel
import com.poe.poegps.feature.presentation.mapper.toOprDisplayable
import com.poe.poegps.feature.presentation.model.OprDisplayable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TpEditorViewModel @Inject constructor(
    private val repository: ObjectsRepository
) : ViewModel() {

    private val _tpObject = MutableStateFlow<OprDisplayable?>(null)
    val tpObject: StateFlow<OprDisplayable?>
        get() = _tpObject

    fun fetchTpObject(tplnr: String, isAbon: Boolean) {
        viewModelScope.launch {
            _tpObject.value = repository.getSingleTpObject(tplnr, isAbon).toOprDisplayable()
        }
    }

    fun saveTpCoord(tp: OprDisplayable) {
        viewModelScope.launch {
            repository.saveTpCoord(tp.toDomainModel())
        }
    }

}