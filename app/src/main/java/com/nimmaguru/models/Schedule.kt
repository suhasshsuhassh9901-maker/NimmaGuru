package com.nimmaguru.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Schedule(
    val id: String = "",
    @SerialName("guru_id")      val guruId: String = "",
    val subject: String = "",
    @SerialName("day_of_week")  val dayOfWeek: String = "",
    @SerialName("start_time")   val startTime: String = "",
    @SerialName("end_time")     val endTime: String = "",
    val location: String = "Samudaya Bhavana",
    @SerialName("max_students") val maxStudents: Int = 10,
    // Date range: stored as ISO strings "yyyy-MM-dd"
    @SerialName("date_from")    val dateFrom: String = "",
    @SerialName("date_to")      val dateTo: String = ""
)