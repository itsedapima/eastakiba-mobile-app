package com.nodimobilebanking.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nodimobilebanking.data.models.LoanTransaction
import com.nodimobilebanking.databinding.ItemTransactionBinding
import com.nodimobilebanking.utils.formatDate
import com.nodimobilebanking.utils.toDisplayAmount

class LoanTransactionAdapter :
    ListAdapter<LoanTransaction, LoanTransactionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemTransactionBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: LoanTransaction) {
            b.tvDate.text = item.trDate.formatDate()
            b.tvDesc.text = item.trDesc.ifBlank { item.loanType }
            b.tvRef.text  = item.trRef

            val debit  = item.debitAmount.toDoubleOrNull()  ?: 0.0
            val credit = item.creditAmount.toDoubleOrNull() ?: 0.0

            if (debit > 0) {
                b.tvAmount.text = item.debitAmount.toDisplayAmount()
                b.tvAmount.setTextColor(b.root.context.getColor(com.nodimobilebanking.R.color.red_500))
            } else {
                b.tvAmount.text = "+${item.creditAmount.toDisplayAmount()}"
                b.tvAmount.setTextColor(b.root.context.getColor(com.nodimobilebanking.R.color.green_500))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<LoanTransaction>() {
            override fun areItemsTheSame(a: LoanTransaction, b: LoanTransaction) = a.id == b.id
            override fun areContentsTheSame(a: LoanTransaction, b: LoanTransaction) = a == b
        }
    }
}
