package com.nimmaguru.ui.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.nimmaguru.R
import com.nimmaguru.SupabaseClient
import com.nimmaguru.databinding.ActivityGuruListBinding
import com.nimmaguru.models.Guru
import com.nimmaguru.ui.base.BaseActivity
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch

class GuruListActivity : BaseActivity() {
    private lateinit var b: ActivityGuruListBinding
    private var allGurus     = listOf<Guru>()
    private var selectedSkill = ""
    private lateinit var adapter: GuruAdapter

    private val skillFilters = listOf("All","Math","Science","English","Kannada",
        "Physics","History","Computer","Carpentry")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityGuruListBinding.inflate(layoutInflater)
        setContentView(b.root)
        attachLangToggle()
        setSupportActionBar(b.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.title_find_guru)
        }

        adapter = GuruAdapter { guru ->
            startActivity(
                Intent(this, GuruDetailActivity::class.java).putExtra("guru_id", guru.id)
            )
        }
        b.rvGurus.layoutManager = LinearLayoutManager(this)
        b.rvGurus.adapter       = adapter

        setupChips()
        loadGurus()

        b.btnSearch.setOnClickListener {
            filter(b.etVillage.text.toString().trim(), selectedSkill)
        }
        b.etVillage.setOnEditorActionListener { _, _, _ ->
            filter(b.etVillage.text.toString().trim(), selectedSkill); true
        }
    }

    private fun setupChips() {
        skillFilters.forEach { skill ->
            val chip = Chip(this).apply {
                text         = skill
                isCheckable  = true
                isChecked    = skill == "All"
                setChipBackgroundColorResource(R.color.chip_bg_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, theme))
                chipCornerRadius = resources.getDimension(R.dimen.chip_radius)
            }
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedSkill = if (skill == "All") "" else skill
                    filter(b.etVillage.text.toString().trim(), selectedSkill)
                }
            }
            b.chipGroup.addView(chip)
        }
    }

    private fun loadGurus() {
        lifecycleScope.launch {
            try {
                b.progressBar.visibility = View.VISIBLE
                allGurus = SupabaseClient.client.postgrest["gurus"].select().decodeList<Guru>()
                b.progressBar.visibility = View.GONE
                adapter.submitList(allGurus)
                b.tvCount.text = "${allGurus.size} ${getString(R.string.gurus_available)}"
            } catch (e: Exception) {
                b.progressBar.visibility = View.GONE
                Toast.makeText(this@GuruListActivity, e.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filter(village: String, skill: String) {
        var list = allGurus
        if (village.isNotEmpty()) list = list.filter { it.village.contains(village, true) }
        if (skill.isNotEmpty())   list = list.filter { it.skills.any { s -> s.contains(skill, true) } }
        adapter.submitList(list)
        b.tvCount.text = "${list.size} ${getString(R.string.gurus_found)}"
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
