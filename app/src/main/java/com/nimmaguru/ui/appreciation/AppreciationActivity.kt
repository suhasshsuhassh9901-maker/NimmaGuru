package com.nimmaguru.ui.appreciation

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityAppreciationBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AppreciationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppreciationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppreciationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val guruId = intent.getStringExtra("guru_id") ?: ""
        val guruName = intent.getStringExtra("guru_name") ?: "Guru"
        supportActionBar?.title = getString(R.string.thank_guru, guruName)
        binding.tvGuruName.text = getString(R.string.send_thanks_to, guruName)

        binding.btnSend.setOnClickListener {
            val studentName = binding.etStudentName.text.toString().trim()
            val message = binding.etMessage.text.toString().trim()

            if (studentName.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, R.string.error_fill_all, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendAppreciation(guruId, studentName, message)
        }
    }

    private fun sendAppreciation(guruId: String, studentName: String, message: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest["appreciations"].insert(
                    buildJsonObject {
                        put("guru_id", guruId)
                        put("student_name", studentName)
                        put("message", message)
                    }
                )

                // Increment thank_you_count using RPC or manual fetch+update
                val guru = SupabaseClient.client.postgrest["gurus"]
                    .select { filter { eq("id", guruId) } }
                    .decodeList<com.nimmaguru.models.Guru>()
                    .firstOrNull()

                if (guru != null) {
                    SupabaseClient.client.postgrest["gurus"].update(
                        buildJsonObject { put("thank_you_count", guru.thankYouCount + 1) }
                    ) { filter { eq("id", guruId) } }
                }

                Toast.makeText(this@AppreciationActivity, R.string.thanks_sent, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AppreciationActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}