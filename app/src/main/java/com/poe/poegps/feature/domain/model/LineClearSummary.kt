package com.poe.poegps.feature.domain.model

/** What clearing a line takes off the phone: its taps (a main line only) and the poles not sent yet. */
data class LineClearSummary(val taps: Int, val unsent: Int)
