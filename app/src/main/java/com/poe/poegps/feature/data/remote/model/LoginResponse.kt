package com.poe.poegps.feature.data.remote.model

import com.google.gson.annotations.SerializedName

data class LoginResponse(val jwt: String) {
    @SerializedName("error")
    var error: String? = null

    @SerializedName("system")
    var system: System? = null

    class System {
        @SerializedName("time")
        var time = 0.0
    }
}