package com.nimmaguru.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Application(
    val id: String = "",
    @SerialName("schedule_id")      val scheduleId: String = "",
    @SerialName("guru_id")          val guruId: String = "",
    @SerialName("student_user_id")  val studentUserId: String? = null,
    @SerialName("student_name")     val studentName: String = "",
    @SerialName("student_phone")    val studentPhone: String? = null,
    val grade: String? = null,
    val note: String? = null,
    val status: String = "pending"
)