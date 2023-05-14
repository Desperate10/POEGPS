package com.poe.poegps.feature.presentation.screens.editor

import androidx.lifecycle.ViewModel
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: ObjectsRepository
): ViewModel() {

    //функция сохранения опор
    //fun saveOprList(list: List<Opr>) = repository.saveOprList(list)

    //функция сохранения линии
    //fun saveLine(lineName: String, tplnr: String, list: List<Opr>) = repository.saveLine(lineName, tplnr, list)

    //функция сохранения ТП
    //fun saveTp(tplnr: String, lat: String, lng: String) = repository.saveTp(tplnr, lat, lng)

    //функция сохранения ПС
    //fun savePs(tplnr: String, lat: String, lng: String) = repository.savePs(tplnr, lat, lng)

    //функция получения списка опор по tplnr
    //fun getOprList(tplnr: String) = repository.getOprList(tplnr)

    //функция получения списка ТП или ПС по конкретной категории напряжения(10 или 0.4)
    //fun getTpList(category: String) = repository.getTpList(category)

}