package com.nodimobilebanking.ui.accounts

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.FragmentAccountsBinding
import com.nodimobilebanking.ui.dashboard.MainActivity
import com.nodimobilebanking.ui.statement.StatementFragment
import com.nodimobilebanking.utils.*
import kotlinx.coroutines.launch

class AccountsFragment : Fragment() {

    private var _binding: FragmentAccountsBinding? = null
    private val binding get() = _binding!!
    private var viewModel: AccountsViewModel? = null
    private var balancesHidden = false
    private val maskedText = "••••••"
    private var lastItems: List<AccountItem> = emptyList()
    private var adapter: AccountsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, s: Bundle?): View? {
        return try {
            _binding = FragmentAccountsBinding.inflate(inflater, c, false)
            binding.root
        } catch (e: Exception) {
            e.printStackTrace()
            inflater.inflate(com.nodimobilebanking.R.layout.fragment_accounts, c, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val tm = TokenManager(requireContext())
            val repo = SaccoRepository(RetrofitClient.getInstance(tm, requireContext()), tm)
            viewModel = ViewModelProvider(this, AccountsViewModel.Factory(repo))[AccountsViewModel::class.java]

            viewLifecycleOwner.lifecycleScope.launch {
                balancesHidden = tm.getBalancesHidden()
                applyBalanceVisibility()
            }

            adapter = AccountsAdapter(
                balancesHidden = balancesHidden,
                onItemClick = { account ->
                    try {
                        val fragment = StatementFragment.forAccount(account.accountCode, account.accountName)
                        (activity as? MainActivity)?.loadFragment(fragment)
                    } catch (_: Exception) {
                        toast("Could not open statement")
                    }
                }
            )

            binding.rvAccounts.layoutManager = LinearLayoutManager(requireContext())
            binding.rvAccounts.adapter = adapter
            binding.swipeRefresh.setOnRefreshListener { viewModel?.load() }

            viewModel?.state?.observe(viewLifecycleOwner) { result ->
                if (_binding == null) return@observe
                try {
                    binding.swipeRefresh.isRefreshing = false
                    when (result) {
                        is Resource.Loading -> {
                            binding.progressBar.visible()
                            binding.tvEmpty.gone()
                        }
                        is Resource.Success -> {
                            binding.progressBar.gone()
                            lastItems = result.data
                            if (lastItems.isEmpty()) {
                                binding.tvEmpty.text = "You have zero balance in all accounts.\nDeposit to view your balances."
                                binding.tvEmpty.visible()
                                binding.rvAccounts.gone()
                            } else {
                                binding.tvEmpty.gone()
                                binding.rvAccounts.visible()
                                adapter?.submitList(lastItems)
                            }
                            applyBalanceVisibility()
                        }
                        is Resource.Error -> {
                            binding.progressBar.gone()
                            binding.tvEmpty.visible()
                            binding.tvEmpty.text = result.message
                        }
                    }
                } catch (_: Exception) {}
            }

            viewModel?.load()
        } catch (e: Exception) {
            toast("Error loading accounts")
        }
    }

    override fun onResume() {
        super.onResume()
        val tm = TokenManager(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val hidden = tm.getBalancesHidden()
            if (hidden != balancesHidden) {
                balancesHidden = hidden
                applyBalanceVisibility()
            }
        }
    }

    private fun applyBalanceVisibility() {
        if (_binding == null) return
        if (balancesHidden) {
            binding.tvSavingsTotal.text = maskedText
            binding.tvLoanTotal.text = maskedText
        } else {
            binding.tvSavingsTotal.text = lastItems.filter { !it.isLoan }.sumOf { it.balance }.formatCurrency()
            binding.tvLoanTotal.text = lastItems.filter { it.isLoan }.sumOf { it.balance }.formatCurrency()
        }
        adapter?.setBalancesHidden(balancesHidden)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
