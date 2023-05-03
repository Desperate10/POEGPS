package gromov.ramdomusertestcase.core.extension

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

fun <T> Fragment.collectLifecycleFlow(flow: (Flow<T?>)?, block: suspend (t: T) -> Unit) {
    flow ?: return
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest {
                it?.also {
                    block(it)
                }
            }
        }
    }
}

fun <T> AppCompatActivity.collectLifecycleFlow(flow: (Flow<T?>)?, block: suspend (t: T) -> Unit) {
    flow ?: return
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect {
                it?.also {
                    block(it)
                }
            }
        }
    }
}