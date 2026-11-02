package com.nodimobilebanking.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nodimobilebanking.data.models.NextOfKin
import com.nodimobilebanking.databinding.ItemNextOfKinBinding

class NextOfKinAdapter : ListAdapter<NextOfKin, NextOfKinAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemNextOfKinBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: NextOfKin) {
            b.tvKinName.text = item.kinName
            b.tvKinRelation.text = item.kinRelationship
            b.tvKinPhone.text = item.kinMobile ?: "—"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemNextOfKinBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<NextOfKin>() {
            override fun areItemsTheSame(a: NextOfKin, b: NextOfKin) = a.id == b.id
            override fun areContentsTheSame(a: NextOfKin, b: NextOfKin) = a == b
        }
    }
}
