package gromov.ramdomusertestcase.core.extension

import androidx.fragment.app.Fragment

fun <T : Any> Fragment.autoCleaned(initializer: (() -> T)? = null): AutoCleanedValue<T> {
    return AutoCleanedValue(this, initializer)
}