package com.nodimobilebanking.ui.dashboard

import androidx.lifecycle.*
import com.nodimobilebanking.data.models.*
import com.nodimobilebanking.data.models.LoanApprovalResponse
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: SaccoRepository) : ViewModel() {

    private val _member = MutableLiveData<Resource<MemberProfile>>()
    val member: LiveData<Resource<MemberProfile>> = _member

    private val _balances = MutableLiveData<Resource<BalanceSummary>>()
    val balances: LiveData<Resource<BalanceSummary>> = _balances

    private val _accounts = MutableLiveData<Resource<List<MemberAccount>>>()
    val accounts: LiveData<Resource<List<MemberAccount>>> = _accounts

    private val _mobileLoanTypes = MutableLiveData<Resource<List<MobileLoanType>>>()
    val mobileLoanTypes: LiveData<Resource<List<MobileLoanType>>> = _mobileLoanTypes

    private val _actionResult = MutableLiveData<Resource<String>?>()
    val actionResult: LiveData<Resource<String>?> = _actionResult

    private val _loanResult = MutableLiveData<Resource<LoanApprovalResponse>?>()
    val loanResult: LiveData<Resource<LoanApprovalResponse>?> = _loanResult

    private val _promotions = MutableLiveData<Resource<List<Promotion>>>()
    val promotions: LiveData<Resource<List<Promotion>>> = _promotions

    fun loadDashboard() {
        viewModelScope.launch {
            _member.value = Resource.Loading()
            _balances.value = Resource.Loading()

            val memberD = async { repository.getMemberProfile() }
            val balD    = async { repository.getBalanceSummary() }

            val memberResult = memberD.await()
            _member.value = memberResult
            _balances.value = balD.await()

            if (memberResult is Resource.Success) {
                _accounts.value = repository.getMemberAccounts()
            }
        }
    }

    fun loadMobileLoanTypes() {
        _mobileLoanTypes.value = Resource.Loading()
        viewModelScope.launch {
            _mobileLoanTypes.value = repository.getMobileLoanTypes()
        }
    }

    fun loadPromotions() {
        viewModelScope.launch { _promotions.value = repository.getPromotions() }
    }

    fun makePayment(memberNo: String, savingType: String, amount: String, desc: String) {
        _actionResult.value = Resource.Loading()
        viewModelScope.launch {
            _actionResult.value = repository.makePayment(memberNo, savingType, amount, desc)
        }
    }

    /** Unified deposit: savings + loan repayments */
    fun makeDeposit(memberNo: String, accountType: String, amount: String, desc: String) {
        _actionResult.value = Resource.Loading()
        viewModelScope.launch {
            _actionResult.value = repository.makeDeposit(memberNo, accountType, amount, desc)
        }
    }

    // ── Loan Quote (charge preview before applying) ─────────────────────────
    private val _loanQuote = MutableLiveData<Resource<LoanQuoteResponse>?>()
    val loanQuote: LiveData<Resource<LoanQuoteResponse>?> = _loanQuote

    fun getLoanQuote(loanType: String, amount: String, period: Int) {
        _loanQuote.value = Resource.Loading()
        viewModelScope.launch {
            _loanQuote.value = repository.loanQuote(loanType, amount, period)
        }
    }
    fun clearLoanQuote() { _loanQuote.value = null }

    fun applyLoan(memberNo: String, amount: String, loanType: String, period: Int, purpose: String, otpCode: String = "") {
        _loanResult.value = Resource.Loading()
        viewModelScope.launch {
            _loanResult.value = repository.applyLoan(memberNo, amount, loanType, period, purpose, otpCode)
        }
    }

    fun clearLoanResult() { _loanResult.value = null }

    fun stkPush(accountType: String, amount: String, accountReference: String = "") {
        _actionResult.value = Resource.Loading()
        viewModelScope.launch {
            _actionResult.value = repository.stkPush(accountType, amount, accountReference)
        }
    }

    // ── Withdrawal (three-step: quote → OTP → confirm + B2C) ────────────────

    private val _withdrawalQuote = MutableLiveData<Resource<WithdrawalQuote>?>()
    val withdrawalQuote: LiveData<Resource<WithdrawalQuote>?> = _withdrawalQuote

    fun getWithdrawalQuote(savingType: String, amount: String) {
        _withdrawalQuote.value = Resource.Loading()
        viewModelScope.launch {
            _withdrawalQuote.value = repository.withdrawalQuote(savingType, amount)
        }
    }

    /** Step 1: call /withdraw WITHOUT otp_code → backend sends OTP. */
    private val _withdrawalOtp = MutableLiveData<Resource<WithdrawalActionResponse>?>()
    val withdrawalOtp: LiveData<Resource<WithdrawalActionResponse>?> = _withdrawalOtp

    fun initiateWithdrawal(savingType: String, amount: String) {
        _withdrawalOtp.value = Resource.Loading()
        viewModelScope.launch {
            _withdrawalOtp.value = repository.makeWithdrawal("", savingType, amount, "Mobile withdrawal")
        }
    }
    fun clearWithdrawalOtp() { _withdrawalOtp.value = null }

    /** Step 2: call /withdraw WITH otp_code → backend verifies, debits, B2C. */
    private val _withdrawalFinal = MutableLiveData<Resource<WithdrawalActionResponse>?>()
    val withdrawalFinal: LiveData<Resource<WithdrawalActionResponse>?> = _withdrawalFinal

    fun confirmWithdrawalWithOtp(savingType: String, amount: String, otpCode: String) {
        _withdrawalFinal.value = Resource.Loading()
        viewModelScope.launch {
            _withdrawalFinal.value = repository.makeWithdrawal("", savingType, amount, "Mobile withdrawal", otpCode)
        }
    }
    fun clearWithdrawalFinal() { _withdrawalFinal.value = null }

    fun clearWithdrawalQuote() { _withdrawalQuote.value = null }

    // ── Dividend slips (list + per-slip PDF download) ─────────────────────
    private val _dividendSlips = MutableLiveData<Resource<DividendSlipsListResponse>?>()
    val dividendSlips: LiveData<Resource<DividendSlipsListResponse>?> = _dividendSlips

    fun loadDividendSlips() {
        _dividendSlips.value = Resource.Loading()
        viewModelScope.launch { _dividendSlips.value = repository.getDividendSlips() }
    }

    fun clearDividendSlips() { _dividendSlips.value = null }

    private val _dividendPdfResult = MutableLiveData<Resource<PdfSaveResult>?>()
    val dividendPdfResult: LiveData<Resource<PdfSaveResult>?> = _dividendPdfResult

    fun downloadDividendSlipPdf(detailId: Int, context: android.content.Context, filename: String) {
        _dividendPdfResult.value = Resource.Loading()
        viewModelScope.launch {
            _dividendPdfResult.value = repository.downloadDividendSlipPdf(detailId, context, filename)
        }
    }

    fun clearDividendPdfResult() { _dividendPdfResult.value = null }

    fun clearActionResult() { _actionResult.value = null }

    class Factory(private val repo: SaccoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return DashboardViewModel(repo) as T
        }
    }
}
