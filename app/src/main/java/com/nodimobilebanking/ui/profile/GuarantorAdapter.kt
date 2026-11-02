package com.nodimobilebanking.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nodimobilebanking.R
import com.nodimobilebanking.data.models.GuarantorEntry
import com.nodimobilebanking.utils.toDisplayAmount

/**
 * Adapter for showing guarantor entries.
 * @param mode "guarantors" → show who guaranteed MY loan
 *             "guarantees" → show whose loan I guaranteed
 */
class GuarantorAdapter(private val mode: String) :
    ListAdapter<GuarantorEntry, GuarantorAdapter.VH>(DIFF) {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvPersonName: TextView = view.findViewById(R.id.tvGuarantorName)
        private val tvLoanInfo: TextView = view.findViewById(R.id.tvGuarantorLoanInfo)
        private val tvAmount: TextView = view.findViewById(R.id.tvGuarantorAmount)
        private val tvLoanType: TextView = view.findViewById(R.id.tvGuarantorLoanType)

        fun bind(item: GuarantorEntry) {
            if (mode == "guarantors") {
                // People who guaranteed MY loan
                tvPersonName.text = item.guarantorName
            } else {
                // Loans I guaranteed for OTHERS
                tvPersonName.text = item.borrowerName
            }
            tvLoanInfo.text = "${item.loanNo} • ${item.loanDate}"
            tvAmount.text = item.amount.toDisplayAmount()
            tvLoanType.text = item.loanType
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_guarantor, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GuarantorEntry>() {
            override fun areItemsTheSame(a: GuarantorEntry, b: GuarantorEntry) = a.id == b.id
            override fun areContentsTheSame(a: GuarantorEntry, b: GuarantorEntry) = a == b
        }
    }
}
