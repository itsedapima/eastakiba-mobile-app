package com.nodimobilebanking.ui.transactions

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.FragmentTransactionsBinding
import com.nodimobilebanking.utils.*

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: TransactionsViewModel
    private lateinit var savingsAdapter: SavingsTransactionAdapter
    private lateinit var loanAdapter: LoanTransactionAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tm = TokenManager(requireContext())
        viewModel = ViewModelProvider(this, TransactionsViewModel.Factory(
            SaccoRepository(RetrofitClient.getInstance(tm, requireContext()), tm)
        ))[TransactionsViewModel::class.java]

        savingsAdapter = SavingsTransactionAdapter()
        loanAdapter    = LoanTransactionAdapter()

        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = savingsAdapter  // default to savings tab

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadAll() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                binding.rvTransactions.adapter = if (tab.position == 0) savingsAdapter else loanAdapter
                updateEmptyState()
            }
            override fun onTabUnselected(t: TabLayout.Tab) {}
            override fun onTabReselected(t: TabLayout.Tab) {}
        })

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.swipeRefresh.isRefreshing = loading
            if (loading) { binding.progressBar.visible(); binding.rvTransactions.gone() }
            else binding.progressBar.gone()
        }

        viewModel.savingsTransactions.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> { savingsAdapter.submitList(result.data); updateEmptyState() }
                is Resource.Error   -> toast(result.message)
                else -> {}
            }
        }

        viewModel.loanTransactions.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> { loanAdapter.submitList(result.data); updateEmptyState() }
                is Resource.Error   -> toast(result.message)
                else -> {}
            }
        }

        viewModel.loadAll()
    }

    private fun updateEmptyState() {
        val isEmpty = if (binding.tabLayout.selectedTabPosition == 0)
            savingsAdapter.itemCount == 0 else loanAdapter.itemCount == 0
        if (isEmpty) { binding.tvEmpty.visible(); binding.rvTransactions.gone() }
        else { binding.tvEmpty.gone(); binding.rvTransactions.visible() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
