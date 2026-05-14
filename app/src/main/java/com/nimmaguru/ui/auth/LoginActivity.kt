package com.nimmaguru.ui.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.SessionManager
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityLoginBinding
import com.nimmaguru.ui.base.BaseActivity
import com.nimmaguru.ui.guru.GuruDashboardActivity
import com.nimmaguru.ui.student.StudentDashboardActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()

        // Animate logo on entry
        b.logoImage.alpha = 0f
        b.tvAppName.alpha = 0f
        b.tvTagline.alpha = 0f
        val logoAnim = ObjectAnimator.ofFloat(b.logoImage, "alpha", 0f, 1f).setDuration(600)
        val nameAnim = ObjectAnimator.ofFloat(b.tvAppName, "alpha", 0f, 1f).setDuration(500)
        val tagAnim  = ObjectAnimator.ofFloat(b.tvTagline, "alpha", 0f, 1f).setDuration(500)
        AnimatorSet().apply {
            play(nameAnim).after(logoAnim)
            play(tagAnim).after(nameAnim)
            start()
        }

        // Auto-login
        lifecycleScope.launch {
            if (SupabaseClient.client.auth.currentSessionOrNull() != null) {
                SessionManager.loadSession()
                navigate()
            }
        }

        b.btnLogin.setOnClickListener {
            val email    = b.etEmail.text.toString().trim()
            val password = b.etPassword.text.toString().trim()
            if (email.isEmpty() || password.isEmpty()) {
                showError(getString(com.nimmaguru.R.string.error_fill_all)); return@setOnClickListener
            }
            setLoading(true)
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signInWith(Email) {
                        this.email    = email
                        this.password = password
                    }
                    SessionManager.loadSession()
                    navigate()
                } catch (e: Exception) {
                    setLoading(false)
                    showError(getString(com.nimmaguru.R.string.error_login_failed))
                }
            }
        }

        b.btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(com.nimmaguru.R.anim.slide_up, android.R.anim.fade_out)
        }
    }

    private fun navigate() {
        val dest = if (SessionManager.currentProfile?.role == "guru")
            GuruDashboardActivity::class.java else StudentDashboardActivity::class.java
        startActivity(Intent(this, dest))
        overridePendingTransition(com.nimmaguru.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun setLoading(on: Boolean) {
        b.progressBar.visibility = if (on) View.VISIBLE else View.GONE
        b.btnLogin.isEnabled = !on
        b.btnLogin.alpha     = if (on) 0.7f else 1f
    }

    private fun showError(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
