package com.nimmaguru.ui.student

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityMyScheduleBinding
import com.nimmaguru.databinding.ItemScheduleStudentBinding
import com.nimmaguru.models.Application
import com.nimmaguru.models.Schedule
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MyScheduleActivity : BaseActivity() {
    private lateinit var b: ActivityMyScheduleBinding
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // approved application schedule IDs → Schedule objects
    private var approvedSchedules = listOf<Schedule>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMyScheduleBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_my_classes)
        }
        b.rvClasses.layoutManager = LinearLayoutManager(this)

        b.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            val dateStr  = sdf.format(picked.time)
            val dayName  = SimpleDateFormat("EEEE", Locale.ENGLISH).format(picked.time)
            showClassesForDay(dateStr, dayName)
        }

        loadApprovedSchedules()
    }

    private fun loadApprovedSchedules() {
        lifecycleScope.launch {
            try {
                b.progressBar.visibility = View.VISIBLE
                val uid  = SupabaseClient.client.auth.currentUserOrNull()?.id ?: return@launch

                val apps = SupabaseClient.client.postgrest["applications"]
                    .select {
                        filter {
                            eq("student_user_id", uid)
                            eq("status", "approved")
                        }
                    }
                    .decodeList<Application>()

                val scheduleIds = apps.map { it.scheduleId }

                if (scheduleIds.isEmpty()) {
                    b.progressBar.visibility = View.GONE
                    b.tvNoClasses.visibility = View.VISIBLE
                    b.tvNoClasses.text = getString(R.string.no_approved_classes)
                    return@launch
                }

                // Fetch each schedule — Supabase postgrest-kt v2 filter `in`
                val schedules = mutableListOf<Schedule>()
                scheduleIds.forEach { sid ->
                    val s = SupabaseClient.client.postgrest["schedules"]
                        .select { filter { eq("id", sid) } }
                        .decodeList<Schedule>()
                    schedules.addAll(s)
                }

                // Conflict check: no two classes same day & overlapping time
                approvedSchedules = resolveConflicts(schedules)
                b.progressBar.visibility = View.GONE

                // Show today by default
                val today   = sdf.format(Date())
                val dayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(Date())
                showClassesForDay(today, dayName)

            } catch (e: Exception) {
                b.progressBar.visibility = View.GONE
                Toast.makeText(this@MyScheduleActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Removes time-conflicting schedules on the same day — keeps earliest added.
     */
    private fun resolveConflicts(list: List<Schedule>): List<Schedule> {
        val kept = mutableListOf<Schedule>()
        list.forEach { candidate ->
            val conflict = kept.any { existing ->
                existing.dayOfWeek == candidate.dayOfWeek &&
                    timesOverlap(existing.startTime, existing.endTime,
                        candidate.startTime, candidate.endTime)
            }
            if (!conflict) kept.add(candidate)
        }
        return kept
    }

    private fun timesOverlap(s1: String, e1: String, s2: String, e2: String): Boolean {
        fun toMin(t: String): Int {
            val (h, m) = t.split(":").map { it.toInt() }
            return h * 60 + m
        }
        return toMin(s1) < toMin(e2) && toMin(s2) < toMin(e1)
    }

    private fun showClassesForDay(dateStr: String, dayName: String) {
        // Filter schedules that include this date and match this day-of-week
        val classes = approvedSchedules.filter { s ->
            s.dayOfWeek.equals(dayName, ignoreCase = true) &&
                dateStr >= s.dateFrom && dateStr <= s.dateTo
        }

        b.tvSelectedDate.text = dateStr
        b.tvNoClasses.visibility = if (classes.isEmpty()) View.VISIBLE else View.GONE
        if (classes.isEmpty()) b.tvNoClasses.text = getString(R.string.no_classes_day)

        b.rvClasses.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
                object : RecyclerView.ViewHolder(
                    ItemScheduleStudentBinding.inflate(
                        LayoutInflater.from(p.context), p, false
                    ).root
                ) {}

            override fun getItemCount() = classes.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                val s  = classes[pos]
                val bv = ItemScheduleStudentBinding.bind(holder.itemView)
                bv.tvDay.text       = s.dayOfWeek
                bv.tvSubject.text   = s.subject
                bv.tvTime.text      = "${s.startTime} – ${s.endTime}"
                bv.tvLocation.text  = s.location
                bv.tvMax.text       = getString(R.string.confirmed_class)
                bv.tvDateRange.text = "${s.dateFrom}  →  ${s.dateTo}"
                bv.btnApply.visibility = View.GONE   // hide apply in own schedule view
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
