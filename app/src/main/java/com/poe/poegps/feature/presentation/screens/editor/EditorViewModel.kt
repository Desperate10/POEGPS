package com.poe.poegps.feature.presentation.screens.editor

import androidx.lifecycle.ViewModel
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: ObjectsRepository
): ViewModel() {

}