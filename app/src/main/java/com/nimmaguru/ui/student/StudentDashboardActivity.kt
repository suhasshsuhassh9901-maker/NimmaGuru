package com.nimmaguru.ui.student

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.R
import com.nimmaguru.SessionManager
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityStudentDashboardBinding
import com.nimmaguru.models.Application
import com.nimmaguru.models.Profile
import com.nimmaguru.ui.auth.LoginActivity
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class StudentDashboardActivity : BaseActivity() {
    private lateinit var b: ActivityStudentDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityStudentDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        b.cardFindGuru.setOnClickListener {
            startActivity(Intent(this, GuruListActivity::class.java))
        }
        b.cardMySchedule.setOnClickListener {
            startActivity(Intent(this, MyScheduleActivity::class.java))
        }

        // Animated sign-out
        b.btnSignOut.setOnClickListener {
            val sx = ObjectAnimator.ofFloat(b.btnSignOut, "scaleX", 1f, 0.93f, 1f).setDuration(180)
            val sy = ObjectAnimator.ofFloat(b.btnSignOut, "scaleY", 1f, 0.93f, 1f).setDuration(180)
            sx.start(); sy.start()
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()
                    SessionManager.clear()
                    startActivity(
                        Intent(this@StudentDashboardActivity, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onResume() { super.onResume(); loadDashboard() }

    private fun loadDashboard() {
        lifecycleScope.launch {
            try {
                val uid     = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val profile = SessionManager.currentProfile
                    ?: SupabaseClient.client.postgrest["profiles"]
                        .select { filter { eq("id", uid) } }
                        .decodeList<Profile>().firstOrNull()

                b.tvName.text = profile?.name ?: getString(R.string.student)

                val initials = (profile?.name ?: "S").split(" ").take(2)
                    .joinToString("") { it.first().uppercase() }
                b.tvInitials.text = initials

                val apps = SupabaseClient.client.postgrest["applications"]
                    .select { filter { eq("student_user_id", uid) } }
                    .decodeList<Application>()

                b.tvTotal.text    = apps.size.toString()
                b.tvPending.text  = apps.count { it.status == "pending" }.toString()
                b.tvApproved.text = apps.count { it.status == "approved" }.toString()

            } catch (e: Exception) {
                Toast.makeText(this@StudentDashboardActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}