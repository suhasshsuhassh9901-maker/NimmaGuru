package com.nimmaguru.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClassSession(
    val id: String = "",
    @SerialName("session_date") val sessionDate: String = "",
    @SerialName("session_time") val sessionTime: String = "",
    val subject: String = "",
    val location: String = ""
)