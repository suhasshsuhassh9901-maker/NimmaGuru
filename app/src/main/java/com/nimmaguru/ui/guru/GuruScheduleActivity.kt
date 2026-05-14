package com.nimmaguru.ui.guru

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityGuruScheduleBinding
import com.nimmaguru.databinding.ItemScheduleGuruBinding
import com.nimmaguru.models.Schedule
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch

class GuruScheduleActivity : BaseActivity() {
    private lateinit var b: ActivityGuruScheduleBinding
    private var guruId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGuruScheduleBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_my_schedule)
        }
        guruId = intent.getStringExtra("guru_id") ?: ""
        b.rvSchedules.layoutManager = LinearLayoutManager(this)

        b.fabAdd.setOnClickListener {
            startActivity(
                Intent(this, AddScheduleActivity::class.java).putExtra("guru_id", guruId)
            )
        }
    }

    override fun onResume() { super.onResume(); loadSchedules() }

    private fun loadSchedules() {
        lifecycleScope.launch {
            try {
                val list = SupabaseClient.client.postgrest["schedules"]
                    .select {
                        filter { eq("guru_id", guruId) }
                        order("day_of_week", Order.ASCENDING)
                        order("start_time", Order.ASCENDING)
                    }
                    .decodeList<Schedule>()

                b.tvEmpty.visibility = if (list.isEmpty()) android.view.View.VISIBLE
                                       else android.view.View.GONE

                b.rvSchedules.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
                        object : RecyclerView.ViewHolder(
                            ItemScheduleGuruBinding.inflate(
                                LayoutInflater.from(p.context), p, false
                            ).root
                        ) {}

                    override fun getItemCount() = list.size

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val s  = list[pos]
                        val bv = ItemScheduleGuruBinding.bind(holder.itemView)
                        bv.tvDay.text      = s.dayOfWeek
                        bv.tvSubject.text  = s.subject
                        bv.tvTime.text     = "${s.startTime} – ${s.endTime}"
                        bv.tvLocation.text = s.location
                        bv.tvMax.text      = "${getString(R.string.max_students)}: ${s.maxStudents}"
                        bv.tvDateRange.text = "${s.dateFrom}  →  ${s.dateTo}"
                        bv.btnDelete.setOnClickListener {
                            AlertDialog.Builder(this@GuruScheduleActivity)
                                .setTitle(getString(R.string.confirm_delete))
                                .setMessage("${s.subject} ${getString(R.string.on)} ${s.dayOfWeek}")
                                .setPositiveButton(getString(R.string.delete)) { _, _ -> deleteSchedule(s.id) }
                                .setNegativeButton(getString(R.string.cancel), null)
                                .show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@GuruScheduleActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteSchedule(id: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest["schedules"]
                    .delete { filter { eq("id", id) } }
                loadSchedules()
            } catch (e: Exception) {
                Toast.makeText(this@GuruScheduleActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
