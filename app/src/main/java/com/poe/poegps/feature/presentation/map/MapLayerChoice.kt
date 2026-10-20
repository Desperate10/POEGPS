package com.poe.poegps.feature.presentation.map

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Street map or satellite on the map screens; remembered on the phone across screens and restarts. */
@Composable
fun rememberSatellite(): MutableState<Boolean> {
    val prefs = LocalContext.current.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    return remember {
        object : MutableState<Boolean> {
            private var backing by mutableStateOf(prefs.getBoolean(KEY_SATELLITE, false))
            override var value: Boolean
                get() = backing
                set(v) {
                    backing = v
                    prefs.edit().putBoolean(KEY_SATELLITE, v).apply()
                }

            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}

private const val PREFS = "map"
private const val KEY_SATELLITE = "satellite"
