package com.poe.poegps.feature.presentation.screens.main

import androidx.lifecycle.ViewModel
import com.poe.poegps.feature.data.remote.model.ObjectState
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ObjectsRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ObjectState>(ObjectState.initial)
    val state : StateFlow<ObjectState> = _state
}