package com.nimmaguru.ui.guru

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityAddScheduleBinding
import com.nimmaguru.databinding.ItemSelectedSlotBinding
import com.nimmaguru.models.Schedule
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.*

class AddScheduleActivity : BaseActivity() {
    private lateinit var b: ActivityAddScheduleBinding
    private var guruId = ""
    private var selectedDateFrom = ""
    private var selectedDateTo   = ""
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    data class TimeSlot(val day: String, val start: String, val end: String)
    private val selectedSlots = mutableListOf<TimeSlot>()

    private fun isOverlapping(newSlot: TimeSlot, existingSlots: List<TimeSlot>): Boolean {
        return existingSlots.any { existing ->
            existing.day == newSlot.day && (newSlot.start < existing.end && newSlot.end > existing.start)
        }
    }

    private val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
    private val sortedTimes = (6..22).flatMap { h ->
        listOf("%02d:00".format(h), "%02d:30".format(h))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddScheduleBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_add_session)
        }
        guruId = intent.getStringExtra("guru_id") ?: ""

        b.spinnerDay.adapter   = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, days)
        b.spinnerStart.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortedTimes)
        b.spinnerEnd.adapter   = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sortedTimes)
            .also { b.spinnerEnd.setSelection(2) }
        b.spinnerMax.adapter   = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            (5..30 step 5).map { "$it ${getString(R.string.students)}" })

        val today = Calendar.getInstance()

        // Date From picker — must be today or future
        b.btnDateFrom.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d) }
                if (picked.before(today.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) })) {
                    Toast.makeText(this, getString(R.string.error_start_past), Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }
                selectedDateFrom = sdf.format(picked.time)
                b.tvDateFrom.text = selectedDateFrom
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.minDate = System.currentTimeMillis() - 1000 }
                .show()
        }

        // Date To picker — must be after dateFrom
        b.btnDateTo.setOnClickListener {
            if (selectedDateFrom.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_pick_start_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val fromCal = Calendar.getInstance().apply {
                time = sdf.parse(selectedDateFrom)!!
                add(Calendar.DAY_OF_MONTH, 1)
            }
            android.app.DatePickerDialog(this, { _, y, m, d ->
                val picked = Calendar.getInstance().apply { set(y, m, d) }
                selectedDateTo = sdf.format(picked.time)
                b.tvDateTo.text = selectedDateTo
            }, fromCal.get(Calendar.YEAR), fromCal.get(Calendar.MONTH), fromCal.get(Calendar.DAY_OF_MONTH))
                .apply { datePicker.minDate = fromCal.timeInMillis }
                .show()
        }

        b.btnAddSlot.setOnClickListener {
            val day = b.spinnerDay.selectedItem.toString()
            val start = b.spinnerStart.selectedItem.toString()
            val end = b.spinnerEnd.selectedItem.toString()

            if (start >= end) {
                Toast.makeText(this, getString(R.string.error_time_order), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newSlot = TimeSlot(day, start, end)
            if (isOverlapping(newSlot, selectedSlots)) {
                Toast.makeText(this, getString(R.string.error_slot_overlap), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            selectedSlots.add(newSlot)
            refreshSlotsUI()
        }

        b.btnSave.setOnClickListener { save() }
    }

    private fun refreshSlotsUI() {
        b.layoutSlots.removeAllViews()
        selectedSlots.forEachIndexed { index, slot ->
            val slotBinding = ItemSelectedSlotBinding.inflate(LayoutInflater.from(this), b.layoutSlots, false)
            slotBinding.tvSlotInfo.text = "${slot.day}: ${slot.start} - ${slot.end}"
            slotBinding.btnRemoveSlot.setOnClickListener {
                selectedSlots.removeAt(index)
                refreshSlotsUI()
            }
            b.layoutSlots.addView(slotBinding.root)
        }
    }

    private fun save() {
        val subject    = b.etSubject.text.toString().trim()
        val location   = b.etLocation.text.toString().trim().ifEmpty { "Samudaya Bhavana" }
        val maxStudents = (b.spinnerMax.selectedItemPosition + 1) * 5

        if (subject.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_subject_required), Toast.LENGTH_SHORT).show(); return
        }
        if (selectedSlots.isEmpty()) {
            Toast.makeText(this, "Please add at least one time slot", Toast.LENGTH_SHORT).show(); return
        }
        if (selectedDateFrom.isEmpty() || selectedDateTo.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_pick_dates), Toast.LENGTH_SHORT).show(); return
        }

        b.btnSave.isEnabled      = false
        b.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Fetch existing schedules to check for overlaps in DB
                val existingFromDb = SupabaseClient.client.postgrest["schedules"]
                    .select { filter { eq("guru_id", guruId) } }
                    .decodeList<Schedule>()
                    .map { TimeSlot(it.dayOfWeek, it.startTime, it.endTime) }

                for (slot in selectedSlots) {
                    if (isOverlapping(slot, existingFromDb)) {
                        Toast.makeText(this@AddScheduleActivity, 
                            "${getString(R.string.error_slot_overlap)}: ${slot.day} ${slot.start}", 
                            Toast.LENGTH_LONG).show()
                        b.btnSave.isEnabled = true
                        b.progressBar.visibility = View.GONE
                        return@launch
                    }
                }

                val objectsToInsert = selectedSlots.map { slot ->
                    buildJsonObject {
                        put("guru_id",     guruId)
                        put("subject",     subject)
                        put("day_of_week", slot.day)
                        put("start_time",  slot.start)
                        put("end_time",    slot.end)
                        put("location",    location)
                        put("max_students", maxStudents)
                        put("date_from",   selectedDateFrom)
                        put("date_to",     selectedDateTo)
                    }
                }

                SupabaseClient.client.postgrest["schedules"].insert(objectsToInsert)

                Toast.makeText(this@AddScheduleActivity, getString(R.string.session_added), Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                b.btnSave.isEnabled      = true
                b.progressBar.visibility = View.GONE
                Toast.makeText(this@AddScheduleActivity, e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
