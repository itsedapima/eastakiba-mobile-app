package com.nodimobilebanking.ui.accounts

import android.view.*
import androidx.recyclerview.widget.*
import com.nodimobilebanking.databinding.ItemAccountBinding
import com.nodimobilebanking.utils.formatCurrency

class AccountsAdapter(
    private var balancesHidden: Boolean = false,
    private val onItemClick: ((AccountItem) -> Unit)? = null
) : ListAdapter<AccountItem, AccountsAdapter.VH>(DIFF) {

    private val maskedText = "••••••"

    fun setBalancesHidden(hidden: Boolean) {
        if (hidden != balancesHidden) {
            balancesHidden = hidden
            notifyItemRangeChanged(0, itemCount)
        }
    }

    inner class VH(private val b: ItemAccountBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: AccountItem) {
            b.tvAccountName.text = item.accountName
            b.tvAccountCode.text = item.subText
            b.tvBalance.text = if (balancesHidden) maskedText else item.balance.formatCurrency()
            val typeLabel = when {
                item.isLoan -> "LOAN"
                item.accountName.contains("share capital", ignoreCase = true) -> "SHARE CAPITAL"
                else -> "SAVINGS"
            }
            b.tvAccountType.text = typeLabel
            b.tvAccountType.setBackgroundColor(
                itemView.context.getColor(
                    if (item.isLoan) com.nodimobilebanking.R.color.red_500 else com.nodimobilebanking.R.color.green_500
                )
            )
            b.root.setOnClickListener { onItemClick?.invoke(item) }
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemAccountBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AccountItem>() {
            override fun areItemsTheSame(a: AccountItem, b: AccountItem) = a.accountCode == b.accountCode
            override fun areContentsTheSame(a: AccountItem, b: AccountItem) = a == b
        }
    }
}
