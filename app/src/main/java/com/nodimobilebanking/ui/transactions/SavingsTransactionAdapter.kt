package com.nodimobilebanking.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nodimobilebanking.data.models.SavingsTransaction
import com.nodimobilebanking.databinding.ItemTransactionBinding
import com.nodimobilebanking.utils.formatDate
import com.nodimobilebanking.utils.toDisplayAmount

class SavingsTransactionAdapter :
    ListAdapter<SavingsTransaction, SavingsTransactionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemTransactionBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: SavingsTransaction) {
            b.tvDate.text    = item.trDate.formatDate()
            b.tvDesc.text    = item.trDesc.ifBlank { item.savingType }
            b.tvRef.text     = item.trRef

            val credit = item.creditAmount.toDoubleOrNull() ?: 0.0
            val debit  = item.debitAmount.toDoubleOrNull()  ?: 0.0

            if (credit > 0) {
                b.tvAmount.text = "+${item.creditAmount.toDisplayAmount()}"
                b.tvAmount.setTextColor(b.root.context.getColor(com.nodimobilebanking.R.color.green_500))
            } else {
                b.tvAmount.text = "-${item.debitAmount.toDisplayAmount()}"
                b.tvAmount.setTextColor(b.root.context.getColor(com.nodimobilebanking.R.color.red_500))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<SavingsTransaction>() {
            override fun areItemsTheSame(a: SavingsTransaction, b: SavingsTransaction) = a.id == b.id
            override fun areContentsTheSame(a: SavingsTransaction, b: SavingsTransaction) = a == b
        }
    }
}
