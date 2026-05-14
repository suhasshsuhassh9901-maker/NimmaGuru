package com.nimmaguru.ui.student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nimmaguru.databinding.ItemGuruCardBinding
import com.nimmaguru.models.Guru

class GuruAdapter(private val onClick: (Guru) -> Unit) :
    ListAdapter<Guru, GuruAdapter.VH>(Diff()) {

    inner class VH(val b: ItemGuruCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemGuruCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val g        = getItem(position)
        val initials = g.name.split(" ").take(2).joinToString("") { it.first().uppercase() }
        holder.b.tvInitials.text  = initials
        holder.b.tvName.text      = g.name
        holder.b.tvVillage.text   = g.village
        holder.b.tvSkills.text    = g.skills.joinToString("  ·  ")
        holder.b.tvLanguage.text  = g.language
        holder.b.tvThanks.text    = "${g.thankYouCount} Thank You's"
        holder.b.root.setOnClickListener { onClick(g) }
    }

    class Diff : DiffUtil.ItemCallback<Guru>() {
        override fun areItemsTheSame(a: Guru, b: Guru)    = a.id == b.id
        override fun areContentsTheSame(a: Guru, b: Guru) = a == b
    }
}
