package com.nimmaguru.ui.student

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityApplyBinding
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ApplyActivity : BaseActivity() {
    private lateinit var b: ActivityApplyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityApplyBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_apply)
        }

        val scheduleId = intent.getStringExtra("schedule_id") ?: ""
        val guruId     = intent.getStringExtra("guru_id")     ?: ""

        b.tvGuruName.text      = "${getString(R.string.applying_to)} ${intent.getStringExtra("guru_name")}"
        b.tvSubject.text       = intent.getStringExtra("subject")
        b.tvScheduleInfo.text  = "${intent.getStringExtra("day")}  ·  ${intent.getStringExtra("time")}"
        b.tvLocation.text      = intent.getStringExtra("location")
        b.tvDateRange.text     = "${intent.getStringExtra("date_from")}  →  ${intent.getStringExtra("date_to")}"

        b.btnApply.setOnClickListener {
            val name  = b.etName.text.toString().trim()
            val phone = b.etPhone.text.toString().trim()
            val grade = b.etGrade.text.toString().trim()
            val note  = b.etNote.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_name_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            b.btnApply.isEnabled      = false
            b.progressBar.visibility  = View.VISIBLE

            lifecycleScope.launch {
                try {
                    val uid = SupabaseClient.client.auth.currentUserOrNull()?.id
                    SupabaseClient.client.postgrest["applications"].insert(
                        buildJsonObject {
                            put("schedule_id",    scheduleId)
                            put("guru_id",        guruId)
                            put("student_name",   name)
                            put("student_phone",  phone)
                            put("grade",          grade)
                            put("note",           note)
                            if (uid != null) put("student_user_id", uid)
                        }
                    )
                    Toast.makeText(
                        this@ApplyActivity,
                        getString(R.string.application_sent),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } catch (e: Exception) {
                    b.btnApply.isEnabled     = true
                    b.progressBar.visibility = View.GONE
                    val msg = if (e.message?.contains("unique") == true)
                        getString(R.string.error_already_applied)
                    else e.message ?: getString(R.string.error_generic)
                    Toast.makeText(this@ApplyActivity, msg, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
