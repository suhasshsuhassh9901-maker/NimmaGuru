package com.nimmaguru.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.R
import com.nimmaguru.SessionManager
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityRegisterBinding
import com.nimmaguru.ui.base.BaseActivity
import com.nimmaguru.ui.guru.GuruEditProfileActivity
import com.nimmaguru.ui.student.StudentDashboardActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegisterActivity : BaseActivity() {
    private lateinit var b: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()

        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_create_account)
        }

        b.btnRegister.setOnClickListener {
            val name    = b.etName.text.toString().trim()
            val email   = b.etEmail.text.toString().trim()
            val pass    = b.etPassword.text.toString().trim()
            val isGuru  = b.rgRole.checkedRadioButtonId == b.rbGuru.id

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_fill_all), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass.length < 6) {
                Toast.makeText(this, getString(R.string.error_password_short), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setLoading(true)

            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signUpWith(Email) {
                        this.email    = email
                        this.password = pass
                    }
                    val uid = SupabaseClient.client.auth.currentUserOrNull()?.id!!
                    SupabaseClient.client.postgrest["profiles"].insert(
                        buildJsonObject {
                            put("id",   uid)
                            put("role", if (isGuru) "guru" else "student")
                            put("name", name)
                        }
                    )
                    SessionManager.loadSession()
                    if (isGuru) {
                        startActivity(
                            Intent(this@RegisterActivity, GuruEditProfileActivity::class.java)
                                .putExtra("is_new", true)
                                .putExtra("prefill_name", name)
                        )
                    } else {
                        startActivity(Intent(this@RegisterActivity, StudentDashboardActivity::class.java))
                    }
                    finish()
                } catch (e: Exception) {
                    setLoading(false)
                    Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    private fun setLoading(on: Boolean) {
        b.progressBar.visibility = if (on) View.VISIBLE else View.GONE
        b.btnRegister.isEnabled  = !on
        b.btnRegister.alpha      = if (on) 0.7f else 1f
    }
}