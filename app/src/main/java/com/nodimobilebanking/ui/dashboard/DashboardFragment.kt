package com.nodimobilebanking.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.nodimobilebanking.R
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.models.MemberAccount
import com.nodimobilebanking.data.models.MemberProfile
import com.nodimobilebanking.data.models.LoanQuoteResponse
import com.nodimobilebanking.data.models.MobileLoanType
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.FragmentDashboardBinding
import com.nodimobilebanking.utils.*
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel

    private var currentMember: MemberProfile? = null
    private var allAccounts: List<MemberAccount> = emptyList()
    private var pendingLoanParams: Map<String, Any>? = null
    private var loadingDlg: LoadingDialog? = null
    private var carouselHandler: Handler? = null
    private var carouselRunnable: Runnable? = null

    // ── Rotating per-account "announcer" state ────────────────────────────
    private var announcerHandler: Handler? = null
    private var announcerRunnable: Runnable? = null
    private var announcerIndex: Int = 0

    // ── Balance visibility toggle ─────────────────────────────────────────
    private var balancesHidden: Boolean = false
    private var lastSavings: Double = 0.0
    private var lastLoans: Double = 0.0
    private var lastTotal: Double = 0.0

    private val maskedText = "••••••"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tm = TokenManager(requireContext())
        viewModel = ViewModelProvider(this, DashboardViewModel.Factory(
            SaccoRepository(RetrofitClient.getInstance(tm, requireContext()), tm)
        ))[DashboardViewModel::class.java]

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }

        binding.btnPay.setOnClickListener { showDepositDialog() }
        binding.btnWithdraw.setOnClickListener { showWithdrawDialog() }
        binding.btnApplyLoan.setOnClickListener { viewModel.loadMobileLoanTypes() }
        binding.btnStatement.setOnClickListener { showStatementOptionsDialog() }
        binding.btnTransactions.setOnClickListener {
            (activity as? MainActivity)?.loadFragment(com.nodimobilebanking.ui.transactions.TransactionsFragment())
        }
        binding.btnDividendSlip.setOnClickListener { viewModel.loadDividendSlips() }

        // Hide / unhide balance details — state persists across navigation
        val tokenMgr = TokenManager(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            balancesHidden = tokenMgr.getBalancesHidden()
            binding.btnToggleBalance.text = if (balancesHidden) "🙈" else "👁"
            renderBalances()
        }
        binding.btnToggleBalance.setOnClickListener {
            balancesHidden = !balancesHidden
            binding.btnToggleBalance.text = if (balancesHidden) "🙈" else "👁"
            renderBalances()
            viewLifecycleOwner.lifecycleScope.launch {
                tokenMgr.saveBalancesHidden(balancesHidden)
            }
        }

        observeData()
        viewModel.loadDashboard()
        setupCarousel()
    }

    private fun setupCarousel() {
        // Start with static fallback — API will override if available
        val fallback = listOf(
            CarouselItem("Save More, Earn More", "Open a fixed deposit today and enjoy higher returns", "#0D2C6D"),
            CarouselItem("Mobile Loans Available", "Get instant loans up to your limit. Apply now!", "#D01B22"),
            CarouselItem("Financial Literacy", "Track your spending and grow your savings", "#1A4DAD"),
        )
        showCarouselItems(fallback)

        // Load promotions from API
        viewModel.promotions.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success && result.data.isNotEmpty()) {
                val colors = listOf("#0D2C6D", "#D01B22", "#1A4DAD", "#A81419", "#091E4A")
                val items = result.data.mapIndexed { i, p ->
                    CarouselItem(
                        title = p.title,
                        subtitle = p.subtitle.ifBlank { p.body.take(80) },
                        bgColor = colors[i % colors.size],
                        imageUrl = p.imageUrl.takeIf { it.isNotBlank() }
                    )
                }
                showCarouselItems(items)
            }
        }
        viewModel.loadPromotions()
    }

    private fun showCarouselItems(items: List<CarouselItem>) {
        val adapter = CarouselAdapter(items)
        binding.carouselPager.adapter = adapter
        binding.carouselPager.setCurrentItem(items.size * 50, false)

        // Auto-scroll every 15 seconds (slower, so users have time to read each update)
        carouselHandler?.removeCallbacksAndMessages(null)
        carouselHandler = Handler(Looper.getMainLooper())
        carouselRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                binding.carouselPager.setCurrentItem(binding.carouselPager.currentItem + 1, true)
                carouselHandler?.postDelayed(this, 15_000)
            }
        }
        carouselHandler?.postDelayed(carouselRunnable!!, 15_000)
    }

    private fun observeData() {
        viewModel.member.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Success -> {
                    currentMember = result.data
                    val name = result.data.firstName?.ifBlank { null }
                        ?: result.data.fullName.split(" ").firstOrNull()
                        ?: "Member"
                    binding.tvWelcome.text = "Hello, $name \uD83D\uDC4B"
                    binding.tvMemberNo.text  = "A/C No: ${result.data.memberNo}"
                }
                is Resource.Error -> toast(result.message)
                else -> {}
            }
        }

        viewModel.balances.observe(viewLifecycleOwner) { result ->
            binding.swipeRefresh.isRefreshing = false
            when (result) {
                is Resource.Loading -> {
                    binding.shimmerLayout.visible(); binding.shimmerLayout.startShimmer()
                    binding.contentLayout.gone()
                }
                is Resource.Success -> {
                    binding.shimmerLayout.stopShimmer(); binding.shimmerLayout.gone()
                    binding.contentLayout.visible()
                    lastSavings = result.data.savingsBalance
                    lastLoans   = result.data.loanBalance
                    lastTotal   = result.data.totalBalance
                    renderBalances()
                }
                is Resource.Error -> {
                    binding.shimmerLayout.stopShimmer(); binding.shimmerLayout.gone()
                    binding.contentLayout.visible()
                    toast(result.message)
                }
            }
        }

        viewModel.accounts.observe(viewLifecycleOwner) { result ->
            if (result is Resource.Success) {
                allAccounts = result.data
                startBalanceAnnouncer()
            }
        }

        // ── Mobile loan types loaded → show dialog ────────────────────────────
        viewModel.mobileLoanTypes.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    if (result.data.isNotEmpty()) showLoanDialog(result.data)
                    else toast("No mobile loan products available")
                }
                is Resource.Error -> toast(result.message)
            }
        }

        // ── Loan quote result (charge preview) ───────────────────────
        viewModel.loanQuote.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            dismissLoadingDialog()
            when (result) {
                is Resource.Loading -> showLoadingDialog("Calculating loan charges...")
                is Resource.Success -> {
                    showLoanQuoteDialog(result.data)
                    viewModel.clearLoanQuote()
                }
                is Resource.Error -> {
                    toast(result.message)
                    viewModel.clearLoanQuote()
                }
            }
        }

        // ── Loan application result (two-step OTP) ─────────────────
        viewModel.loanResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            when (result) {
                is Resource.Loading -> {}
                is Resource.Success -> {
                    dismissLoadingDialog()
                    val r = result.data
                    if (r.otpSent) {
                        showLoanOtpDialog(r.message)
                    } else {
                        toast(r.message)
                        pendingLoanParams = null
                        viewModel.loadDashboard()
                    }
                    viewModel.clearLoanResult()
                }
                is Resource.Error -> {
                    dismissLoadingDialog()
                    toast(result.message)
                    viewModel.clearLoanResult()
                }
            }
        }

        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            dismissLoadingDialog()
            when (result) {
                is Resource.Loading -> showLoadingDialog("Processing...")
                is Resource.Success -> {
                    toast(result.data)
                    viewModel.loadDashboard()
                    viewModel.clearActionResult()
                }
                is Resource.Error -> {
                    toast(result.message)
                    viewModel.clearActionResult()
                }
            }
        }

        // ── Withdrawal quote → confirmation dialog ─────────────────────────
        viewModel.withdrawalQuote.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            dismissLoadingDialog()
            when (result) {
                is Resource.Loading -> showLoadingDialog("Getting charges...")
                is Resource.Success -> {
                    showWithdrawalConfirmDialog(result.data)
                    viewModel.clearWithdrawalQuote()
                }
                is Resource.Error -> {
                    toast(result.message)
                    viewModel.clearWithdrawalQuote()
                }
            }
        }

        // ── Withdrawal Step 1 result (OTP sent) ─────────────────────────────
        viewModel.withdrawalOtp.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            dismissLoadingDialog()
            when (result) {
                is Resource.Loading -> showLoadingDialog("Sending confirmation code...")
                is Resource.Success -> {
                    val r = result.data
                    if (r.otpSent) {
                        showWithdrawalOtpDialog(r.message)
                    } else {
                        toast(r.message)
                        viewModel.loadDashboard()
                    }
                    viewModel.clearWithdrawalOtp()
                }
                is Resource.Error -> {
                    toast(result.message)
                    viewModel.clearWithdrawalOtp()
                }
            }
        }

        // ── Withdrawal Step 2 result (B2C final) ────────────────────────────
        viewModel.withdrawalFinal.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            dismissLoadingDialog()
            when (result) {
                is Resource.Loading -> showLoadingDialog("Processing withdrawal...")
                is Resource.Success -> {
                    toast(result.data.message)
                    pendingWithdrawalAccountId = ""
                    pendingWithdrawalAmount = ""
                    viewModel.loadDashboard()
                    viewModel.clearWithdrawalFinal()
                }
                is Resource.Error -> {
                    toast(result.message)
                    viewModel.clearWithdrawalFinal()
                }
            }
        }

        // ── Dividend slips list ─────────────────────────────────────────────
        viewModel.dividendSlips.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            dismissLoadingDialog()
            when (result) {
                is Resource.Loading -> showLoadingDialog("Fetching your dividend slips...")
                is Resource.Success -> {
                    showDividendSlipsList(result.data)
                    viewModel.clearDividendSlips()
                }
                is Resource.Error -> {
                    toast(result.message)
                    viewModel.clearDividendSlips()
                }
            }
        }

        // ── Dividend PDF download result ────────────────────────────────────
        viewModel.dividendPdfResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            dismissLoadingDialog()
            when (result) {
                is Resource.Loading -> showLoadingDialog("Downloading slip to Downloads folder...")
                is Resource.Success -> {
                    openPdf(result.data)
                    viewModel.clearDividendPdfResult()
                }
                is Resource.Error -> {
                    toast(result.message)
                    viewModel.clearDividendPdfResult()
                }
            }
        }
    }

    // ── WITHDRAWAL CONFIRMATION ──────────────────────────────────────────────
    private var pendingWithdrawalAccountId = ""
    private var pendingWithdrawalAmount = ""

    private fun showWithdrawalConfirmDialog(quote: com.nodimobilebanking.data.models.WithdrawalQuote) {
        val ctx = requireContext()
        val sb = StringBuilder().apply {
            append("Account: ").append(quote.accountName).append("\n")
            append("Balance: KES ").append("%,.2f".format(quote.balance)).append("\n\n")
            append("Amount: KES ").append("%,.2f".format(quote.amount)).append("\n")
            if (quote.bandFee > 0) append("Fee: KES ").append("%,.2f".format(quote.bandFee)).append("\n")
            quote.extraCharges.forEach { append(it.name).append(": KES ").append("%,.2f".format(it.amount)).append("\n") }
            append("\nTotal charges: KES ").append("%,.2f".format(quote.totalCharges)).append("\n")
            append("You receive: KES ").append("%,.2f".format(quote.netReceived)).append("\n")
            append("Debit: KES ").append("%,.2f".format(quote.grossDebit))
        }
        if (quote.insufficientBalance) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setTitle("Insufficient Balance").setMessage(sb.toString())
                .setPositiveButton("OK", null).show()
            return
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
            .setTitle("Confirm Withdrawal").setMessage(sb.toString())
            .setPositiveButton("Send to M-Pesa") { _, _ ->
                // Step 1: backend validates, sends OTP, returns otp_sent=true
                showLoadingDialog("Sending confirmation code...")
                viewModel.initiateWithdrawal(pendingWithdrawalAccountId, pendingWithdrawalAmount)
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showWithdrawalOtpDialog(message: String) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Enter 6-digit code"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
            gravity = android.view.Gravity.CENTER
            textSize = 24f
            setTextColor(resources.getColor(com.nodimobilebanking.R.color.black, null))
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Withdrawal")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Confirm & Send") { _, _ ->
                val otp = input.text.toString().trim()
                if (otp.length != 6) { toast("Enter valid 6-digit code"); return@setPositiveButton }
                showLoadingDialog("Verifying & sending to M-Pesa...")
                viewModel.confirmWithdrawalWithOtp(
                    pendingWithdrawalAccountId,
                    pendingWithdrawalAmount,
                    otp
                )
            }
            .setNegativeButton("Cancel") { _, _ ->
                pendingWithdrawalAccountId = ""
                pendingWithdrawalAmount = ""
            }
            .setCancelable(false)
            .show()
    }

    // ── DIVIDEND SLIPS — list latest first, tap to download PDF ──────────────
    private fun showDividendSlipsList(data: com.nodimobilebanking.data.models.DividendSlipsListResponse) {
        if (!data.hasDividends || data.slips.isEmpty()) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dividend Slips")
                .setMessage("You don't have any dividend slips yet.")
                .setPositiveButton("OK", null).show()
            return
        }
        // Build labels: "2025 · Savings Deposit  ·  KES 1,234.56"
        val labels = data.slips.map { s ->
            "${s.year} · ${s.savingTypeDisplay}\nNet: KES ${"%,.2f".format(s.netPayout)}"
        }.toTypedArray()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle("Your Dividend Slips")
            .setItems(labels) { _, which ->
                val slip = data.slips[which]
                downloadSlipPdf(slip)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun downloadSlipPdf(slip: com.nodimobilebanking.data.models.DividendSlipRow) {
        val safeBatch = slip.batchNo.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val filename = "DividendSlip_${slip.year}_${safeBatch}.pdf"
        viewModel.downloadDividendSlipPdf(slip.id, requireContext(), filename)
    }

    private fun openPdf(saved: com.nodimobilebanking.data.models.PdfSaveResult) {
        toast("Saved to ${saved.displayPath}")
        try {
            // The MediaStore / file Uri is already shareable — no FileProvider needed
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(saved.uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(android.content.Intent.createChooser(intent, "Open Dividend Slip"))
        } catch (e: Exception) {
            // No PDF viewer installed — the toast already told the user where it lives
        }
    }

    // ── DEPOSIT DIALOG — Deposits to ALL accounts (savings + loan repayments) ─
    private fun showDepositDialog() {
        val member = currentMember ?: run { toast("Profile not loaded yet"); return }

        // Build combined list: savings accounts + active loan accounts
        val accounts = allAccounts.ifEmpty {
            listOf(MemberAccount("regular", "Regular Savings"))
        }
        val labels = accounts.map { it.text ?: it.id }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_action, null)
        val spinnerAccount = dialogView.findViewById<Spinner>(R.id.spinnerAccount)
        val etAmount  = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val etDesc    = dialogView.findViewById<TextInputEditText>(R.id.etDesc)
        val tvTitle   = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        tvTitle.text = "Deposit or pay a loan via M-Pesa"
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogSubtitle)
        tvSubtitle?.text = "An M-Pesa prompt will be sent to your phone"
        // Hide description field
        dialogView.findViewById<android.view.View>(R.id.tilDesc)?.visibility = android.view.View.GONE
        spinnerAccount.adapter = styledSpinnerAdapter(labels)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Send M-Pesa Prompt") { _, _ ->
                val amount = etAmount.text.toString().trim()
                if (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
                    toast("Enter a valid amount"); return@setPositiveButton
                }
                val account = accounts[spinnerAccount.selectedItemPosition]
                // Build account_reference: e.g. "01146SC", "01146ML"
                val initials = account.accInitials?.takeIf { it.isNotBlank() }
                    ?: account.accountCode?.takeIf { it.isNotBlank() } ?: ""
                val accountRef = "${member.memberNo}${initials}"
                viewModel.stkPush(account.id, amount, accountRef)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── WITHDRAW DIALOG — Savings only ────────────────────────────────────────
    private fun showWithdrawDialog() {
        currentMember ?: run { toast("Profile not loaded yet"); return }
        val accounts = allAccounts.filter { !it.isLoan && it.isWithdrawable }.ifEmpty {
            listOf(MemberAccount("regular", "Regular Savings"))
        }
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_action, null)
        val spinnerAccount = dialogView.findViewById<Spinner>(R.id.spinnerAccount)
        val etAmount  = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val tvTitle   = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvSubtitle = dialogView.findViewById<TextView>(R.id.tvDialogSubtitle)

        tvTitle.text = "Withdraw to M-Pesa"
        tvSubtitle?.text = "Funds will be sent to your registered M-Pesa number"
        // Hide description field for withdrawals
        dialogView.findViewById<android.view.View>(R.id.tilDesc)?.visibility = android.view.View.GONE
        spinnerAccount.adapter = styledSpinnerAdapter(accounts.map { it.text.orEmpty() })

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val amount = etAmount.text.toString().trim()
                if (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
                    toast("Enter a valid amount"); return@setPositiveButton
                }
                val account = accounts[spinnerAccount.selectedItemPosition]
                pendingWithdrawalAccountId = account.id
                pendingWithdrawalAmount = amount
                viewModel.getWithdrawalQuote(account.id, amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── LOAN DIALOG — With limits, period constraints, mobile loan types ──────
    private fun showLoanDialog(loanTypes: List<MobileLoanType>) {
        val member = currentMember ?: run { toast("Profile not loaded yet"); return }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_loan, null)
        val spinnerType = dialogView.findViewById<Spinner>(R.id.spinnerLoanType)
        val etAmount    = dialogView.findViewById<TextInputEditText>(R.id.etLoanAmount)
        val etPeriod    = dialogView.findViewById<TextInputEditText>(R.id.etLoanPeriod)
        val etPurpose   = dialogView.findViewById<TextInputEditText>(R.id.etLoanPurpose)
        val tvLimitInfo = dialogView.findViewById<TextView>(R.id.tvLimitInfo)

        val labels = loanTypes.map { "${it.accountName}" }
        spinnerType.adapter = styledSpinnerAdapter(labels)

        // Update limit info — compact display with charges
        fun updateLimitDisplay(pos: Int) {
            val loan = loanTypes[pos]
            tvLimitInfo?.let {
                it.visible()
                val sb = StringBuilder()
                sb.append("Limit: ${loan.availableLimit.formatCurrency()} | Max Period: ${loan.maxRepaymentPeriod} months")
                if (loan.charges.isNotEmpty()) {
                    sb.append("\nCharges: ")
                    sb.append(loan.charges.joinToString(", ") { c ->
                        // Backend now sends `percentage` / `flat_amount` (lowercase, snake_case)
                        // but tolerate any older variants too.
                        val type = c.chargeType.lowercase().replace(" ", "_")
                        val amt = c.amount
                        if (type.startsWith("percent"))
                            "${c.name} ${String.format("%.2f", amt)}%"
                        else
                            "${c.name} KES ${String.format("%,.2f", amt)}"
                    })
                }
                it.text = sb.toString()
            }
            // Issue 4: the max period is already shown above (`Max: 6 months`),
            // so DO NOT duplicate it on the input field hint.
            etPeriod.hint = "Period (months)"
        }

        spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateLimitDisplay(pos)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        if (loanTypes.isNotEmpty()) updateLimitDisplay(0)

        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val amount  = etAmount.text.toString().trim()
                val period  = etPeriod.text.toString().trim().toIntOrNull() ?: 0
                val purpose = etPurpose.text.toString().trim().ifBlank { "Mobile loan application" }
                val selectedLoan = loanTypes[spinnerType.selectedItemPosition]

                // Validate amount
                if (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
                    toast("Enter a valid amount"); return@setPositiveButton
                }
                // Validate against available limit
                if (amount.toDouble() > selectedLoan.availableLimit) {
                    toast("Amount exceeds available limit of ${selectedLoan.availableLimit.formatCurrency()}")
                    return@setPositiveButton
                }
                // Validate period
                if (period < 1) {
                    toast("Enter loan period in months"); return@setPositiveButton
                }
                if (period > selectedLoan.maxRepaymentPeriod) {
                    toast("Period cannot exceed ${selectedLoan.maxRepaymentPeriod} months")
                    return@setPositiveButton
                }

                // Save params for later steps (OTP)
                pendingLoanParams = mapOf(
                    "memberNo" to member.memberNo, "amount" to amount,
                    "loanType" to selectedLoan.accountType,
                    "period" to period, "purpose" to purpose
                )
                // Step 0: Get charge breakdown first — no OTP, no disbursement
                showLoadingDialog("Getting loan quote...")
                viewModel.getLoanQuote(selectedLoan.accountType, amount, period)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Shows the loan charge breakdown. "Apply" triggers OTP step. */
    private fun showLoanQuoteDialog(quote: LoanQuoteResponse) {
        val params = pendingLoanParams ?: return
        val ctx = requireContext()
        val sb = StringBuilder().apply {
            append("Loan: ").append(quote.accountName).append("\n")
            append("Principal: KES ").append("%,.2f".format(quote.principal)).append("\n\n")
            // Charge breakdown
            quote.charges.forEach { c ->
                val name = c["name"]?.toString() ?: ""
                val amt = (c["amount"] as? Number)?.toDouble() ?: 0.0
                val rate = c["rate"]?.toString() ?: ""
                append("• ").append(name)
                if (rate.isNotBlank()) append(" (").append(rate).append(")")
                append(": KES ").append("%,.2f".format(amt)).append("\n")
            }
            append("\nTotal charges: KES ").append("%,.2f".format(quote.totalCharges)).append("\n")
            append("You receive: KES ").append("%,.2f".format(quote.netDisbursed)).append("\n")
            append("Installment: KES ").append("%,.2f".format(quote.installment))
            append(" × ").append(quote.period).append(" months\n")
            append("Interest rate: ").append("%.1f".format(quote.interestRate)).append("%")
        }

        MaterialAlertDialogBuilder(ctx)
            .setTitle("Loan Charges Breakdown")
            .setMessage(sb.toString())
            .setPositiveButton("Apply") { _, _ ->
                // Step 1: validate + send OTP (no otp_code)
                showLoadingDialog("Submitting loan application...")
                viewModel.applyLoan(
                    params["memberNo"] as String,
                    params["amount"] as String,
                    params["loanType"] as String,
                    params["period"] as Int,
                    params["purpose"] as String
                )
            }
            .setNegativeButton("Cancel") { _, _ -> pendingLoanParams = null }
            .show()
    }

    private fun showLoadingDialog(message: String) {
        dismissLoadingDialog()
        loadingDlg = showLoading(message)
    }

    private fun dismissLoadingDialog() {
        loadingDlg?.dismiss()
        loadingDlg = null
    }

    private fun showLoanOtpDialog(message: String) {
        val params = pendingLoanParams ?: return
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Enter 6-digit code"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            maxLines = 1
            filters = arrayOf(android.text.InputFilter.LengthFilter(6))
            gravity = android.view.Gravity.CENTER
            textSize = 24f
            setTextColor(resources.getColor(com.nodimobilebanking.R.color.black, null))
            setPadding(40, 30, 40, 30)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Loan Application")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Confirm & Disburse") { _, _ ->
                val otp = input.text.toString().trim()
                if (otp.length != 6) { toast("Enter valid 6-digit code"); return@setPositiveButton }
                showLoadingDialog("Verifying code & processing loan...")
                viewModel.applyLoan(
                    params["memberNo"] as String,
                    params["amount"] as String,
                    params["loanType"] as String,
                    params["period"] as Int,
                    params["purpose"] as String,
                    otp  // Step 2: with OTP
                )
            }
            .setNegativeButton("Cancel") { _, _ -> pendingLoanParams = null }
            .setCancelable(false)
            .show()
    }

    // ── ACCOUNT STATEMENT — navigate to unified statement screen ────────────────
    private fun showStatementOptionsDialog() {
        (activity as? MainActivity)?.loadFragment(
            com.nodimobilebanking.ui.statement.StatementFragment()
        )
    }

    override fun onDestroyView() {
        dismissLoadingDialog()
        carouselRunnable?.let { carouselHandler?.removeCallbacks(it) }
        carouselHandler = null
        carouselRunnable = null
        announcerRunnable?.let { announcerHandler?.removeCallbacks(it) }
        announcerHandler = null
        announcerRunnable = null
        super.onDestroyView()
        _binding = null
    }

    // ── Balance rendering (respects hide/unhide toggle) ──────────────────────
    private fun renderBalances() {
        if (_binding == null) return
        if (balancesHidden) {
            binding.tvSavingsBalance.text = maskedText
            binding.tvLoanBalance.text    = maskedText
            binding.tvTotalBalance.text   = maskedText
            // Also mask the announcer label to not leak loan details
            binding.tvAccountLabel.text   = "Your balance is"
        } else {
            binding.tvSavingsBalance.text = lastSavings.formatCurrency()
            binding.tvLoanBalance.text    = lastLoans.formatCurrency()
            updateAnnouncerDisplay(animate = false)
        }
    }

    // ── Rotating per-account "train-station announcer" ───────────────────────
    private fun startBalanceAnnouncer() {
        if (_binding == null) return
        announcerRunnable?.let { announcerHandler?.removeCallbacks(it) }
        announcerHandler = Handler(Looper.getMainLooper())
        announcerIndex = 0
        updateAnnouncerDisplay(animate = false)
        announcerRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return
                val accounts = announcerAccounts()
                if (accounts.isNotEmpty()) {
                    announcerIndex = (announcerIndex + 1) % accounts.size
                    updateAnnouncerDisplay(animate = true)
                }
                announcerHandler?.postDelayed(this, 15_000)
            }
        }
        announcerHandler?.postDelayed(announcerRunnable!!, 15_000)
    }

    private fun announcerAccounts(): List<MemberAccount> = allAccounts

    private fun updateAnnouncerDisplay(animate: Boolean) {
        if (_binding == null) return
        val accounts = announcerAccounts()

        // When hidden, show a generic label without any account names or amounts
        if (balancesHidden) {
            binding.tvAccountLabel.text = "Your balance is"
            binding.tvTotalBalance.text = maskedText
            return
        }

        val (label, amount) = if (accounts.isEmpty()) {
            "Your savings balance" to lastSavings
        } else {
            val acc = accounts[announcerIndex % accounts.size]
            val name = acc.accountName?.ifBlank { null } ?: acc.text ?: "Account"
            "Your $name balance is" to acc.balance
        }
        val amountText = amount.formatCurrency()

        if (animate) {
            val labelView = binding.tvAccountLabel
            val amountView = binding.tvTotalBalance
            labelView.animate().alpha(0f).setDuration(350).withEndAction {
                if (_binding == null) return@withEndAction
                labelView.text = label
                labelView.animate().alpha(1f).setDuration(350).start()
            }.start()
            amountView.animate().alpha(0f).setDuration(350).withEndAction {
                if (_binding == null) return@withEndAction
                amountView.text = amountText
                amountView.animate().alpha(1f).setDuration(350).start()
            }.start()
        } else {
            binding.tvAccountLabel.text = label
            binding.tvTotalBalance.text = amountText
        }
    }

    /**
     * Builds an ArrayAdapter that uses our custom spinner layouts with explicit
     * dark-on-light colors, so dropdowns stay legible regardless of the device's
     * dark-mode setting.
     */
    private fun styledSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, items)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        return adapter
    }
}
