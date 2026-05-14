package com.nimmaguru.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Guru(
    val id: String = "",
    @SerialName("user_id")      val userId: String = "",
    val name: String = "",
    val skills: List<String> = emptyList(),
    val village: String = "",
    val phone: String? = null,
    val bio: String? = null,
    val language: String = "English",
    @SerialName("thank_you_count") val thankYouCount: Int = 0
)