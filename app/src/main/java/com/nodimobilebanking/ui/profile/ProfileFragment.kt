package com.nodimobilebanking.ui.profile

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.FragmentProfileBinding
import com.nodimobilebanking.ui.dashboard.MainActivity
import com.nodimobilebanking.utils.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ProfileViewModel
    private lateinit var kinsAdapter: NextOfKinAdapter
    private lateinit var guarantorsAdapter: GuarantorAdapter
    private lateinit var guaranteesAdapter: GuarantorAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tm = TokenManager(requireContext())
        viewModel = ViewModelProvider(
            this,
            ProfileViewModel.Factory(SaccoRepository(RetrofitClient.getInstance(tm, requireContext()), tm))
        )[ProfileViewModel::class.java]

        // Setup kins RecyclerView
        kinsAdapter = NextOfKinAdapter()
        binding.rvKins.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKins.adapter = kinsAdapter

        // Setup guarantors RecyclerView
        guarantorsAdapter = GuarantorAdapter("guarantors")
        binding.rvGuarantors.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGuarantors.adapter = guarantorsAdapter

        // Setup guarantees RecyclerView
        guaranteesAdapter = GuarantorAdapter("guarantees")
        binding.rvGuarantees.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGuarantees.adapter = guaranteesAdapter

        // Collapsible section headers
        setupCollapsible(binding.tvKinsHeader, binding.layoutKinsContent, "Next of Kins")
        setupCollapsible(binding.tvGuarantorsHeader, binding.layoutGuarantorsContent, "My Guarantors")
        setupCollapsible(binding.tvGuaranteesHeader, binding.layoutGuaranteesContent, "My Guarantees")

        viewModel.loadProfile()

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ -> (activity as? MainActivity)?.logout() }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // ── Observe profile ───────────────────────────────────────────────────
        viewModel.profile.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> binding.progressBar.visible()
                is Resource.Success -> {
                    binding.progressBar.gone()
                    val c = result.data
                    binding.tvFullName.text    = c.displayName
                    binding.tvUsername.text     = "Member #${c.memberNo}"
                    binding.tvMemberNo.text      = "Account: ${c.memberNo}"
                    binding.tvEmail.text       = c.displayEmail
                    binding.tvPhone.text       = c.mobile
                    binding.tvRole.text        = c.memberType
                        .replace("_", " ")
                        .split(" ")
                        .joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }
                    binding.tvMemberSince.text = "Member since: ${c.regDate}"
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.tvFullName.text = "Could not load profile"
                    toast(result.message)
                }
            }
        }

        // ── Observe next of kins ──────────────────────────────────────────────
        viewModel.kins.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val kinsList = result.data
                    binding.tvKinsHeader.visible()
                    if (kinsList.isNotEmpty()) {
                        binding.rvKins.visible()
                        binding.tvNoKins.gone()
                        kinsAdapter.submitList(kinsList)
                    } else {
                        binding.rvKins.gone()
                        binding.tvNoKins.visible()
                    }
                }
                is Resource.Error -> {
                    binding.tvKinsHeader.gone()
                    binding.layoutKinsContent.gone()
                }
            }
        }

        // ── Observe MY GUARANTORS (people who guaranteed my loans) ────────────
        viewModel.myGuarantors.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val list = result.data
                    binding.tvGuarantorsHeader.visible()
                    if (list.isNotEmpty()) {
                        binding.rvGuarantors.visible()
                        binding.tvNoGuarantors.gone()
                        guarantorsAdapter.submitList(list)
                    } else {
                        binding.rvGuarantors.gone()
                        binding.tvNoGuarantors.visible()
                    }
                }
                is Resource.Error -> {
                    binding.tvGuarantorsHeader.gone()
                    binding.layoutGuarantorsContent.gone()
                }
            }
        }

        // ── Observe MY GUARANTEES (loans I guaranteed for others) ─────────────
        viewModel.myGuarantees.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    val list = result.data
                    binding.tvGuaranteesHeader.visible()
                    if (list.isNotEmpty()) {
                        binding.rvGuarantees.visible()
                        binding.tvNoGuarantees.gone()
                        guaranteesAdapter.submitList(list)
                    } else {
                        binding.rvGuarantees.gone()
                        binding.tvNoGuarantees.visible()
                    }
                }
                is Resource.Error -> {
                    binding.tvGuaranteesHeader.gone()
                    binding.layoutGuaranteesContent.gone()
                }
            }
        }
    }

    private fun setupCollapsible(header: android.widget.TextView, content: View, label: String) {
        header.setOnClickListener {
            val expanded = content.visibility == View.VISIBLE
            if (expanded) {
                content.gone()
                header.text = "▶ $label"
            } else {
                content.visible()
                header.text = "▼ $label"
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
