package com.nimmaguru.ui.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityApplicationsBinding
import com.nimmaguru.databinding.ItemApplicationBinding
import com.nimmaguru.models.Application
import com.nimmaguru.models.Schedule
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ApplicationsActivity : BaseActivity() {
    private lateinit var b: ActivityApplicationsBinding
    private var guruId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityApplicationsBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_student_requests)
        }
        guruId = intent.getStringExtra("guru_id") ?: ""
        b.rvApplications.layoutManager = LinearLayoutManager(this)
        loadApplications()
    }

    private fun loadApplications() {
        lifecycleScope.launch {
            try {
                val apps = SupabaseClient.client.postgrest["applications"]
                    .select {
                        filter { eq("guru_id", guruId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Application>()

                val schedules = SupabaseClient.client.postgrest["schedules"]
                    .select { filter { eq("guru_id", guruId) } }
                    .decodeList<Schedule>()
                val scheduleMap = schedules.associateBy { it.id }

                b.tvEmpty.visibility = if (apps.isEmpty()) android.view.View.VISIBLE
                                       else android.view.View.GONE

                b.rvApplications.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, vt: Int) =
                        object : RecyclerView.ViewHolder(
                            ItemApplicationBinding.inflate(LayoutInflater.from(p.context), p, false).root
                        ) {}

                    override fun getItemCount() = apps.size

                    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
                        val app = apps[pos]
                        val sch = scheduleMap[app.scheduleId]
                        val bv  = ItemApplicationBinding.bind(holder.itemView)

                        bv.tvStudentName.text = app.studentName
                        bv.tvPhone.text       = app.studentPhone ?: getString(R.string.no_phone)
                        bv.tvGrade.text       = if (app.grade != null) "${getString(R.string.grade)}: ${app.grade}" else ""
                        bv.tvNote.text        = app.note ?: ""
                        bv.tvSession.text     = if (sch != null)
                            "${sch.subject}  ·  ${sch.dayOfWeek}  ${sch.startTime}–${sch.endTime}"
                        else getString(R.string.unknown_session)

                        val (colorRes, labelRes) = when (app.status) {
                            "approved" -> Pair(0xFF2E7D32.toInt(), R.string.status_approved)
                            "rejected" -> Pair(0xFFC62828.toInt(), R.string.status_rejected)
                            else       -> Pair(0xFFE65100.toInt(), R.string.status_pending)
                        }
                        bv.tvStatus.text = getString(labelRes)
                        bv.tvStatus.setTextColor(colorRes)

                        val isPending = app.status == "pending"
                        bv.btnApprove.visibility = if (isPending) android.view.View.VISIBLE else android.view.View.GONE
                        bv.btnReject.visibility  = if (isPending) android.view.View.VISIBLE else android.view.View.GONE

                        bv.btnApprove.setOnClickListener { updateStatus(app.id, "approved") }
                        bv.btnReject.setOnClickListener  { updateStatus(app.id, "rejected") }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@ApplicationsActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStatus(appId: String, status: String) {
        lifecycleScope.launch {
            try {
                SupabaseClient.client.postgrest["applications"]
                    .update(buildJsonObject { put("status", status) }) {
                        filter { eq("id", appId) }
                    }
                loadApplications()
            } catch (e: Exception) {
                Toast.makeText(this@ApplicationsActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
