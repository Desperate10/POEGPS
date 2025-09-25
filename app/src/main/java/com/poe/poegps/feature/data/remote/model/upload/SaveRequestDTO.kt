package com.poe.poegps.feature.data.remote.model.upload

import com.poe.poegps.feature.data.remote.model.LinePillarDTO
import com.poe.poegps.feature.data.remote.model.RecloserDTO

data class SaveRequestDTO(
    val pillarList: List<LinePillarDTO>,
    val recloserList: List<RecloserDTO> = emptyList()
)
