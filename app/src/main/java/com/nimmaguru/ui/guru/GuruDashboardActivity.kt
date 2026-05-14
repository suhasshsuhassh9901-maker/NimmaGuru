package com.nimmaguru.ui.guru

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.R
import com.nimmaguru.SessionManager
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityGuruDashboardBinding
import com.nimmaguru.models.Application
import com.nimmaguru.models.Guru
import com.nimmaguru.models.Schedule
import com.nimmaguru.ui.auth.LoginActivity
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class GuruDashboardActivity : BaseActivity() {
    private lateinit var b: ActivityGuruDashboardBinding
    private var guruId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGuruDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.title = getString(R.string.title_dashboard)

        b.cardEditProfile.setOnClickListener {
            startActivity(Intent(this, GuruEditProfileActivity::class.java))
        }
        b.cardSchedule.setOnClickListener {
            startActivity(Intent(this, GuruScheduleActivity::class.java).putExtra("guru_id", guruId))
        }
        b.cardApplications.setOnClickListener {
            startActivity(Intent(this, ApplicationsActivity::class.java).putExtra("guru_id", guruId))
        }

        // Animated sign-out button
        b.btnSignOut.setOnClickListener {
            animateSignOut()
            lifecycleScope.launch {
                try {
                    SupabaseClient.client.auth.signOut()
                    SessionManager.clear()
                    startActivity(
                        Intent(this@GuruDashboardActivity, LoginActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out)
                } catch (_: Exception) {}
            }
        }
    }

    private fun animateSignOut() {
        val scaleX = ObjectAnimator.ofFloat(b.btnSignOut, "scaleX", 1f, 0.93f, 1f)
        val scaleY = ObjectAnimator.ofFloat(b.btnSignOut, "scaleY", 1f, 0.93f, 1f)
        scaleX.duration = 200; scaleY.duration = 200
        scaleX.start(); scaleY.start()
    }

    override fun onResume() { super.onResume(); loadDashboard() }

    private fun loadDashboard() {
        lifecycleScope.launch {
            try {
                val uid  = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch
                val guru = SupabaseClient.client.postgrest["gurus"]
                    .select { filter { eq("user_id", uid) } }
                    .decodeList<Guru>().firstOrNull()

                if (guru == null) {
                    startActivity(
                        Intent(this@GuruDashboardActivity, GuruEditProfileActivity::class.java)
                            .putExtra("is_new", true)
                    )
                    return@launch
                }

                guruId = guru.id
                SessionManager.currentGuru = guru

                b.tvName.text    = guru.name
                b.tvVillage.text = guru.village
                b.tvSkills.text  = guru.skills.joinToString("  ·  ")
                b.tvThanks.text  = "${guru.thankYouCount} ${getString(R.string.thank_yous)}"

                val initials = guru.name.split(" ").take(2).joinToString("") { it.first().uppercase() }
                b.tvInitials.text = initials

                val schedules = SupabaseClient.client.postgrest["schedules"]
                    .select { filter { eq("guru_id", guruId) } }
                    .decodeList<Schedule>()
                b.tvScheduleCount.text = "${schedules.size} ${getString(R.string.sessions)}"

                val apps = SupabaseClient.client.postgrest["applications"]
                    .select { filter { eq("guru_id", guruId); eq("status", "pending") } }
                    .decodeList<Application>()
                b.tvPendingCount.text  = "${apps.size} ${getString(R.string.pending)}"
                b.badgePending.text    = apps.size.toString()
                b.badgePending.visibility = if (apps.isNotEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                Toast.makeText(this@GuruDashboardActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}