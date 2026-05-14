package com.nimmaguru

import com.nimmaguru.models.Guru
import com.nimmaguru.models.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest

object SessionManager {
    var currentProfile: Profile? = null
    var currentGuru: Guru? = null

    suspend fun loadSession() {
        val userId = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return
        currentProfile = SupabaseClient.client.postgrest["profiles"]
            .select { filter { eq("id", userId) } }
            .decodeList<Profile>().firstOrNull()
        if (currentProfile?.role == "guru") {
            currentGuru = SupabaseClient.client.postgrest["gurus"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<Guru>().firstOrNull()
        }
    }

    fun clear() {
        currentProfile = null
        currentGuru = null
    }
}
