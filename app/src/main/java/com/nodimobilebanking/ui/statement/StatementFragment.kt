package com.nodimobilebanking.ui.statement

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.nodimobilebanking.R
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.models.AccountStatement
import com.nodimobilebanking.data.models.MemberAccount
import com.nodimobilebanking.data.models.FullStatementResponse
import com.nodimobilebanking.data.models.StatementLine
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.FragmentStatementBinding
import com.nodimobilebanking.ui.dashboard.MainActivity
import com.nodimobilebanking.utils.*
import com.nodimobilebanking.utils.PdfStatementGenerator
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class StatementFragment : Fragment() {

    companion object {
        private const val ARG_ACCOUNT_TYPE = "account_type"
        private const val ARG_ACCOUNT_NAME = "account_name"
        private const val ARG_PDF_ONLY     = "pdf_only"

        fun forAccount(accountType: String, accountName: String): StatementFragment {
            return StatementFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ACCOUNT_TYPE, accountType)
                    putString(ARG_ACCOUNT_NAME, accountName)
                }
            }
        }

        fun forPdfDownload(): StatementFragment {
            return StatementFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_PDF_ONLY, true)
                }
            }
        }
    }

    private var _binding: FragmentStatementBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StatementViewModel

    private var singleAccountType: String? = null
    private var singleAccountName: String? = null
    private var isPdfShortcut = false
    private val isSingleAccountMode get() = singleAccountType != null

    private var fromDate: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
    private var toDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private var allAccounts: List<MemberAccount> = emptyList()
    private var selectedAccountId: String = ""
    private var includeZeroBalance = false
    private var statementLoaded = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentStatementBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        singleAccountType = arguments?.getString(ARG_ACCOUNT_TYPE)
        singleAccountName = arguments?.getString(ARG_ACCOUNT_NAME)
        isPdfShortcut = arguments?.getBoolean(ARG_PDF_ONLY, false) == true

        val tm = TokenManager(requireContext())
        viewModel = ViewModelProvider(this, StatementViewModel.Factory(
            SaccoRepository(RetrofitClient.getInstance(tm, requireContext()), tm)
        ))[StatementViewModel::class.java]

        binding.btnBack.setOnClickListener {
            val main = activity as? MainActivity ?: return@setOnClickListener
            val bottomNav = main.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
            if (isSingleAccountMode) {
                main.loadFragment(com.nodimobilebanking.ui.accounts.AccountsFragment())
            } else {
                bottomNav?.selectedItemId = R.id.nav_dashboard
            }
        }

        binding.btnDownload.setOnClickListener { generatePdf() }

        if (isSingleAccountMode) {
            setupSingleAccountMode()
        } else {
            fromDate = Calendar.getInstance().apply {
                set(Calendar.MONTH, Calendar.JANUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            setupFullStatementMode()
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SINGLE ACCOUNT MODE
    // ══════════════════════════════════════════════════════════════════════════
    private fun setupSingleAccountMode() {
        binding.tvTitle.text = "Member Statement"
        binding.dateFilterLayout.visible()
        binding.cardBalanceSummary.gone()
        binding.accountSelectorLayout.gone()

        updateDateDisplay()
        viewModel.loadData()

        viewModel.member.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                val c = result.data
                binding.tvMemberHeader.text = "Statement - ${c.displayName} (${c.memberNo})"
            }
        }

        fetchSingleAccountStatement()

        viewModel.accountStatement.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> { binding.progressBar.visible(); binding.btnDownload.gone() }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    statementLoaded = true
                    binding.btnDownload.visible()
                    displayAccountStatementPreview(result.data)
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    toast(result.message)
                }
            }
        }

        binding.etFromDate.setOnClickListener {
            pickDate(fromDate) { fromDate = it; updateDateDisplay() }
        }
        binding.etToDate.setOnClickListener {
            pickDate(toDate) { toDate = it; updateDateDisplay() }
        }
        binding.btnFetch.setOnClickListener { fetchSingleAccountStatement() }
    }

    private fun fetchSingleAccountStatement() {
        singleAccountType?.let {
            viewModel.loadAccountStatement(
                it,
                dateFormat.format(fromDate.time),
                dateFormat.format(toDate.time)
            )
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FULL STATEMENT MODE
    // ══════════════════════════════════════════════════════════════════════════
    private fun setupFullStatementMode() {
        binding.tvTitle.text = "Member Statement"
        binding.dateFilterLayout.visible()
        binding.cardBalanceSummary.gone()
        binding.accountSelectorLayout.visible()

        updateDateDisplay()

        binding.etFromDate.setOnClickListener { pickDate(fromDate) { fromDate = it; updateDateDisplay() } }
        binding.etToDate.setOnClickListener { pickDate(toDate) { toDate = it; updateDateDisplay() } }
        binding.btnFetch.setOnClickListener { fetchStatement() }

        binding.switchIncludeZero.setOnCheckedChangeListener { _, checked ->
            includeZeroBalance = checked
            populateAccountSpinner()
        }

        binding.spinnerAccount.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedAccountId = if (pos == 0) "" else {
                    val filtered = filteredAccounts()
                    if (pos - 1 < filtered.size) filtered[pos - 1].id else ""
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewModel.loadData()

        viewModel.member.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                val c = result.data
                binding.tvMemberHeader.text = "Statement - ${c.displayName} (${c.memberNo})"
            }
        }

        viewModel.accounts.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                allAccounts = result.data
                populateAccountSpinner()
            }
        }

        viewModel.fullStatement.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> { binding.progressBar.visible(); binding.btnDownload.gone() }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    statementLoaded = true
                    binding.btnDownload.visible()
                    displayFullStatementPreview(result.data)
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    toast(result.message)
                }
            }
        }

        viewModel.accountStatement.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> { binding.progressBar.visible(); binding.btnDownload.gone() }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    statementLoaded = true
                    binding.btnDownload.visible()
                    displayAccountStatementPreview(result.data)
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    toast(result.message)
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading) binding.progressBar.visible() else binding.progressBar.gone()
            binding.btnFetch.isEnabled = !loading
        }
        viewModel.error.observe(viewLifecycleOwner) { it?.let { toast(it) } }
    }

    private fun filteredAccounts(): List<MemberAccount> {
        return if (includeZeroBalance) allAccounts
        else allAccounts.filter { it.balance != 0.0 || it.isLoan }
    }

    private fun populateAccountSpinner() {
        val accounts = filteredAccounts()
        val labels = mutableListOf("All Accounts (Full Statement)")
        accounts.forEach { acc ->
            val name = acc.accountName?.takeIf { it.isNotBlank() }
                ?: acc.text?.substringAfter("-", acc.text ?: "")?.substringBefore("(")?.trim()
                ?: acc.id
            val label = if (acc.isLoan && acc.id.isNotBlank()) {
                "$name(${acc.id})"
            } else {
                val code = if ((acc.subText ?: "").contains("(") && (acc.subText ?: "").contains(")")) {
                    acc.subText!!.substringAfter("(").substringBefore(")")
                } else {
                    acc.accountCode?.takeIf { it.isNotBlank() } ?: ""
                }
                if (code.isNotBlank()) "$name ($code)" else name
            }
            labels.add(label)
        }
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, labels)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerAccount.adapter = adapter
    }

    private fun fetchStatement() {
        val from = dateFormat.format(fromDate.time)
        val to = dateFormat.format(toDate.time)
        if (selectedAccountId.isEmpty()) {
            viewModel.loadFullStatement(from, to)
        } else {
            viewModel.loadAccountStatement(selectedAccountId, from, to)
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // STATEMENT PREVIEW — table rendering
    // ══════════════════════════════════════════════════════════════════════════

    private fun displayAccountStatementPreview(stmt: AccountStatement) {
        binding.tvStatementPreview.gone()
        val container = getOrCreatePreviewContainer()
        container.removeAllViews()
        container.visible()

        addPreviewHeader(container, stmt.accountName, stmt.periodFrom, stmt.periodTo)
        addBbfRow(container, stmt.balanceBroughtForward)
        stmt.lines.forEach { line -> addTransactionRow(container, line) }
        addClosingRow(container, stmt.closingBalance)
    }

    private fun displayFullStatementPreview(full: FullStatementResponse) {
        binding.tvStatementPreview.gone()
        val container = getOrCreatePreviewContainer()
        container.removeAllViews()
        container.visible()

        if (full.accounts.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "No transactions found for this period."
                setTextColor(resources.getColor(R.color.text_secondary, null))
                setPadding(dp(16), dp(24), dp(16), dp(24))
                textSize = 14f
            }
            container.addView(empty)
            return
        }

        full.accounts.forEach { acct ->
            addPreviewHeader(container, acct.accountName, acct.periodFrom, acct.periodTo)
            addBbfRow(container, acct.balanceBroughtForward)
            acct.lines.forEach { line -> addTransactionRow(container, line) }
            addClosingRow(container, acct.closingBalance)
            val spacer = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(12)
                )
            }
            container.addView(spacer)
        }
    }

    private fun getOrCreatePreviewContainer(): LinearLayout {
        val tag = "previewContainer"
        val parent = binding.tvStatementPreview.parent as? ViewGroup ?: return LinearLayout(requireContext())
        var container = parent.findViewWithTag<LinearLayout>(tag)
        if (container == null) {
            container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                this.tag = tag
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(16) }
                setBackgroundColor(resources.getColor(R.color.white, null))
                setPadding(0, 0, 0, dp(8))
            }
            val idx = parent.indexOfChild(binding.tvStatementPreview) + 1
            parent.addView(container, idx)
        }
        return container
    }

    private fun addPreviewHeader(container: LinearLayout, name: String, from: String, to: String) {
        val header = TextView(requireContext()).apply {
            text = name
            setTextColor(resources.getColor(R.color.white, null))
            setBackgroundColor(resources.getColor(R.color.primary, null))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }
        container.addView(header)

        val period = TextView(requireContext()).apply {
            text = "Period: $from  to  $to"
            setTextColor(resources.getColor(R.color.text_secondary, null))
            textSize = 11f
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundColor(resources.getColor(R.color.surface, null))
        }
        container.addView(period)

        addColumnHeaders(container)
    }

    private fun addColumnHeaders(container: LinearLayout) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(resources.getColor(R.color.surface, null))
        }
        val cols = listOf("Date" to 2.2f, "Description" to 3.2f, "Debit" to 1.8f, "Credit" to 1.8f, "Running Balance" to 2.3f)
        cols.forEachIndexed { idx, (label, weight) ->
            val tv = TextView(requireContext()).apply {
                text = label
                textSize = 10f
                setTypeface(null, Typeface.BOLD)
                setTextColor(resources.getColor(R.color.text_secondary, null))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                if (idx >= 2) gravity = Gravity.END
                if (idx == 0) setPadding(dp(2), 0, dp(4), 0)
            }
            row.addView(tv)
        }
        container.addView(row)
        addDivider(container)
    }

    private fun addBbfRow(container: LinearLayout, bbf: Double) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(resources.getColor(R.color.accent_50, null))
        }
        val dateTv = TextView(requireContext()).apply {
            text = ""
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.2f)
            textSize = 10f
        }
        val descTv = TextView(requireContext()).apply {
            text = "Balance B/F"
            setTypeface(null, Typeface.BOLD_ITALIC)
            setTextColor(resources.getColor(R.color.text_primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3.5f)
            textSize = 10f
        }
        val drTv = emptyCell(1.8f)
        val crTv = emptyCell(1.8f)
        val balTv = TextView(requireContext()).apply {
            text = bbf.formatAmount()
            setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            gravity = Gravity.END
            textSize = 10f
        }
        row.addView(dateTv); row.addView(descTv); row.addView(drTv); row.addView(crTv); row.addView(balTv)
        container.addView(row)
        addDivider(container)
    }

    private fun addTransactionRow(container: LinearLayout, line: StatementLine) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }

        val dateStr = try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(line.date)
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(parsed!!)
        } catch (_: Exception) { line.date }

        val dateTv = TextView(requireContext()).apply {
            text = dateStr
            textSize = 9f
            setTextColor(resources.getColor(R.color.text_secondary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2.2f)
            setPadding(dp(2), 0, dp(4), 0)
        }

        val descTv = TextView(requireContext()).apply {
            text = line.description ?: line.ref ?: "-"
            textSize = 9f
            setTextColor(resources.getColor(R.color.text_primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3.5f)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val drTv = TextView(requireContext()).apply {
            text = if (line.debit != null && line.debit > 0) line.debit.formatAmount() else ""
            textSize = 9f
            setTextColor(resources.getColor(R.color.red_500, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f)
            gravity = Gravity.END
        }

        val crTv = TextView(requireContext()).apply {
            text = if (line.credit != null && line.credit > 0) line.credit.formatAmount() else ""
            textSize = 9f
            setTextColor(resources.getColor(R.color.green_500, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f)
            gravity = Gravity.END
        }

        val balTv = TextView(requireContext()).apply {
            text = line.balance.formatAmount()
            textSize = 9f
            setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text_primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            gravity = Gravity.END
        }

        row.addView(dateTv); row.addView(descTv); row.addView(drTv); row.addView(crTv); row.addView(balTv)
        container.addView(row)
        addDivider(container, light = true)
    }

    private fun addClosingRow(container: LinearLayout, closing: Double) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(8), dp(6), dp(8))
            setBackgroundColor(resources.getColor(R.color.surface, null))
        }
        val spacer = emptyCell(2.2f)
        val label = TextView(requireContext()).apply {
            text = "Closing Balance"
            setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3.5f)
            textSize = 10f
        }
        val drTv = emptyCell(1.8f)
        val crTv = emptyCell(1.8f)
        val balTv = TextView(requireContext()).apply {
            text = closing.formatAmount()
            setTypeface(null, Typeface.BOLD)
            setTextColor(resources.getColor(R.color.primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            gravity = Gravity.END
            textSize = 10f
        }
        row.addView(spacer); row.addView(label); row.addView(drTv); row.addView(crTv); row.addView(balTv)
        container.addView(row)
    }

    private fun emptyCell(weight: Float) = TextView(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
    }

    private fun addDivider(container: LinearLayout, light: Boolean = false) {
        val div = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { marginStart = dp(6); marginEnd = dp(6) }
            setBackgroundColor(resources.getColor(
                if (light) R.color.divider else R.color.text_secondary, null
            ))
            alpha = if (light) 0.3f else 0.15f
        }
        container.addView(div)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // ══════════════════════════════════════════════════════════════════════════
    // DATE PICKER
    // ══════════════════════════════════════════════════════════════════════════

    private fun updateDateDisplay() {
        binding.etFromDate.setText(dateFormat.format(fromDate.time))
        binding.etToDate.setText(dateFormat.format(toDate.time))
    }

    private fun pickDate(current: Calendar, onPicked: (Calendar) -> Unit) {
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            onPicked(cal)
        }, current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show()
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PDF GENERATION
    // ══════════════════════════════════════════════════════════════════════════

    private fun generatePdf() {
        val member = (viewModel.member.value as? Resource.Success)?.data
            ?: run { toast("Profile not loaded"); return }


        binding.btnDownload.isEnabled = false
        binding.progressBar.visible()

        val loading = showLoading("Generating PDF statement...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tm = TokenManager(requireContext())
                val apiCompanyInfo = if (!isSingleAccountMode && selectedAccountId.isEmpty()) {
                    (viewModel.fullStatement.value as? Resource.Success)?.data?.companyInfo
                } else {
                    (viewModel.accountStatement.value as? Resource.Success)?.data?.companyInfo
                }

                val companyName    = apiCompanyInfo?.companyName?.takeIf { it.isNotBlank() }
                    ?: try { tm.getCompanyName() } catch (_: Exception) { "SACCO MIS" }
                val companyAddress = apiCompanyInfo?.companyAddress?.takeIf { it.isNotBlank() }
                    ?: try { tm.getCompanyAddress() } catch (_: Exception) { "" }
                val companyContact = apiCompanyInfo?.companyContact?.takeIf { it.isNotBlank() }
                    ?: try { tm.getCompanyContact() } catch (_: Exception) { "" }
                val companyLocation = apiCompanyInfo?.companyLocation?.takeIf { it.isNotBlank() }
                    ?: try { tm.getCompanyLocation() } catch (_: Exception) { "" }

                val file = if (isSingleAccountMode) {
                    val stmt = (viewModel.accountStatement.value as? Resource.Success)?.data
                        ?: throw Exception("Statement not loaded yet. Tap Generate first.")
                    PdfStatementGenerator.generateAccountStatement(
                        requireContext(), member, stmt,
                        companyName, companyAddress, companyContact, companyLocation
                    )
                } else if (selectedAccountId.isNotEmpty()) {
                    val stmt = (viewModel.accountStatement.value as? Resource.Success)?.data
                        ?: throw Exception("Statement not loaded yet. Tap Generate first.")
                    PdfStatementGenerator.generateAccountStatement(
                        requireContext(), member, stmt,
                        companyName, companyAddress, companyContact, companyLocation
                    )
                } else {
                    val fullStmt = (viewModel.fullStatement.value as? Resource.Success)?.data
                    if (fullStmt != null) {
                        PdfStatementGenerator.generateConsolidatedStatement(
                            requireContext(), member, fullStmt,
                            companyName, companyAddress, companyContact, companyLocation
                        )
                    } else {
                        val summary = (viewModel.balanceSummary.value as? Resource.Success)?.data
                            ?: throw Exception("Balance not loaded")
                        PdfStatementGenerator.generateStatement(
                            requireContext(), member,
                            summary.savingsTransactions, summary.loanTransactions,
                            summary.savingsBalance, summary.loanBalance,
                            companyName, companyAddress, companyContact, companyLocation
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.gone(); binding.btnDownload.isEnabled = true; loading.dismiss()
                    val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    PdfStatementGenerator.copyToDownloads(requireContext(), file)
                    if (intent.resolveActivity(requireActivity().packageManager) != null)
                        startActivity(Intent.createChooser(intent, "Open Statement"))
                    else toast("PDF saved to Downloads: ${file.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.gone(); binding.btnDownload.isEnabled = true; loading.dismiss()
                    toast("PDF error: ${e.localizedMessage}")
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
