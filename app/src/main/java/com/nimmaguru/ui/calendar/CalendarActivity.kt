package com.nimmaguru.ui.calendar

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityCalendarBinding
import com.nimmaguru.models.ClassSession
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Class Calendar"

        loadSessions()
    }

    private fun loadSessions() {
        lifecycleScope.launch {
            try {
                val sessions = SupabaseClient.client.postgrest["sessions"]
                    .select { order("session_date", Order.ASCENDING) }
                    .decodeList<ClassSession>()

                if (sessions.isEmpty()) {
                    binding.tvEmpty.text = "No sessions scheduled yet."
                    return@launch
                }

                val sb = StringBuilder()
                sessions.forEach { s ->
                    sb.append("📅 ${s.sessionDate} at ${s.sessionTime}\n")
                    sb.append("📚 Subject: ${s.subject}\n")
                    sb.append("📍 ${s.location}\n")
                    sb.append("─────────────────────\n")
                }
                binding.tvSessions.text = sb.toString()

            } catch (e: Exception) {
                Toast.makeText(this@CalendarActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}