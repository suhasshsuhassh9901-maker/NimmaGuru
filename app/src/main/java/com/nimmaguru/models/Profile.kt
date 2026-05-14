package com.nimmaguru.models

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val role: String = "",
    val name: String = ""
)