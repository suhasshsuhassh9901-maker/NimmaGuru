package com.nimmaguru.ui.student

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityGuruDetailBinding
import com.nimmaguru.databinding.ItemScheduleStudentBinding
import com.nimmaguru.models.Guru
import com.nimmaguru.models.Schedule
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

class GuruDetailActivity : BaseActivity() {
    private lateinit var b: ActivityGuruDetailBinding
    private var guruId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGuruDetailBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply { setDisplayHomeAsUpEnabled(true); title = "" }
        guruId = intent.getStringExtra("guru_id") ?: return
        b.rvSchedules.layoutManager = LinearLayoutManager(this)
        loadDetail()
    }

    private fun loadDetail() {
        lifecycleScope.launch {
            try {
                val guru = SupabaseClient.client.postgrest["gurus"]
                    .select { filter { eq("id", guruId) } }.decodeList<Guru>().first()

                val initials = guru.name.split(" ").take(2).joinToString("") { it.first().uppercase() }
                b.tvInitials.text    = initials
                b.tvName.text        = guru.name
                b.tvVillage.text     = guru.village
                b.tvSkills.text      = guru.skills.joinToString("  ·  ")
                b.tvBio.text         = guru.bio ?: getString(R.string.default_bio)
                b.tvLanguage.text    = guru.language
                b.tvPhone.text       = guru.phone ?: getString(R.string.not_listed)
                b.tvThanks.text      = "${guru.thankYouCount} ${getString(R.string.thank_yous)}"

                val schedules = SupabaseClient.client.postgrest["schedules"]
                    .select {
                        filter { eq("guru_id", guruId) }
                        order("day_of_week", Order.ASCENDING)
                        order("start_time", Order.ASCENDING)
                    }
                    .decodeList<Schedule>()

                b.tvScheduleCount.text = "${schedules.size} ${getString(R.string.sessions_available)}"

                val orderedDays = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
                val grouped     = schedules.groupBy { it.dayOfWeek }
                val items       = orderedDays.flatMap { grouped[it] ?: emptyList() }

                b.rvSchedules.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
                        object : RecyclerView.ViewHolder(
                            ItemScheduleStudentBinding.inflate(
                                LayoutInflater.from(p.context), p, false
                            ).root
                        ) {}

                    override fun getItemCount() = items.size

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val s  = items[pos]
                        val bv = ItemScheduleStudentBinding.bind(holder.itemView)
                        bv.tvDay.text       = s.dayOfWeek
                        bv.tvSubject.text   = s.subject
                        bv.tvTime.text      = "${s.startTime} – ${s.endTime}"
                        bv.tvLocation.text  = s.location
                        bv.tvMax.text       = "${getString(R.string.up_to)} ${s.maxStudents} ${getString(R.string.students)}"
                        bv.tvDateRange.text = "${s.dateFrom}  →  ${s.dateTo}"
                        bv.btnApply.setOnClickListener {
                            startActivity(
                                Intent(this@GuruDetailActivity, ApplyActivity::class.java)
                                    .putExtra("schedule_id", s.id)
                                    .putExtra("guru_id",     guruId)
                                    .putExtra("guru_name",   guru.name)
                                    .putExtra("subject",     s.subject)
                                    .putExtra("day",         s.dayOfWeek)
                                    .putExtra("time",        "${s.startTime} – ${s.endTime}")
                                    .putExtra("location",    s.location)
                                    .putExtra("date_from",   s.dateFrom)
                                    .putExtra("date_to",     s.dateTo)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@GuruDetailActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
