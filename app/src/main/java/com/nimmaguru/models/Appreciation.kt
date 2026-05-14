package com.nimmaguru.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Appreciation(
    val id: String = "",
    @SerialName("guru_id") val guruId: String = "",
    @SerialName("student_name") val studentName: String = "",
    val message: String = ""
)