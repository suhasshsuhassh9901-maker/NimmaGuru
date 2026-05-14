package com.nimmaguru.ui.guru

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.SessionManager
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityGuruEditProfileBinding
import com.nimmaguru.models.Guru
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class GuruEditProfileActivity : AppCompatActivity() {
    private lateinit var b: ActivityGuruEditProfileBinding
    private var existingId: String? = null
    private var isNew = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGuruEditProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        isNew = intent.getBooleanExtra("is_new", false)
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(!isNew)
            title = if (isNew) "Create Profile" else "Edit Profile"
        }

        if (isNew) {
            b.tvSubtitle.text = "Tell students who you are and what you teach"
            b.etName.setText(intent.getStringExtra("prefill_name") ?: "")
        } else {
            b.tvSubtitle.text = "Keep your profile up to date"
            loadExisting()
        }

        b.btnSave.setOnClickListener { saveProfile() }
    }

    private fun loadExisting() {
        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val guru = SupabaseClient.client.postgrest["gurus"]
                    .select { filter { eq("user_id", uid) } }
                    .decodeList<Guru>().firstOrNull() ?: return@launch
                existingId = guru.id
                b.etName.setText(guru.name)
                b.etVillage.setText(guru.village)
                b.etSkills.setText(guru.skills.joinToString(", "))
                b.etPhone.setText(guru.phone ?: "")
                b.etBio.setText(guru.bio ?: "")
                b.spinnerLanguage.setSelection(listOf("English","Kannada","Both").indexOf(guru.language).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }

    private fun saveProfile() {
        val name = b.etName.text.toString().trim()
        val village = b.etVillage.text.toString().trim()
        val skillsRaw = b.etSkills.text.toString().trim()
        val phone = b.etPhone.text.toString().trim()
        val bio = b.etBio.text.toString().trim()
        val lang = b.spinnerLanguage.selectedItem.toString()

        if (name.isEmpty() || village.isEmpty() || skillsRaw.isEmpty()) {
            Toast.makeText(this, "Name, village and skills are required", Toast.LENGTH_SHORT).show()
            return
        }
        val skills = skillsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        b.btnSave.isEnabled = false
        b.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val uid = SupabaseClient.client.auth.currentUserOrNull()?.id!!
                val json = buildJsonObject {
                    put("name", name)
                    putJsonArray("skills") { skills.forEach { add(JsonPrimitive(it)) } }
                    put("village", village)
                    put("phone", phone)
                    put("bio", bio)
                    put("language", lang)
                }
                if (existingId == null) {
                    SupabaseClient.client.postgrest["gurus"].insert(
                        buildJsonObject {
                            put("user_id", uid)
                            put("name", name)
                            putJsonArray("skills") { skills.forEach { add(JsonPrimitive(it)) } }
                            put("village", village)
                            put("phone", phone)
                            put("bio", bio)
                            put("language", lang)
                        }
                    )
                } else {
                    SupabaseClient.client.postgrest["gurus"].update(json) {
                        filter { eq("id", existingId!!) }
                    }
                }
                SessionManager.loadSession()
                Toast.makeText(this@GuruEditProfileActivity, "Profile saved!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@GuruEditProfileActivity, GuruDashboardActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                finish()
            } catch (e: Exception) {
                b.btnSave.isEnabled = true
                b.progressBar.visibility = View.GONE
                Toast.makeText(this@GuruEditProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}