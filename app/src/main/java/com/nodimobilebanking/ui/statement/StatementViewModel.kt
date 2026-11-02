package com.nodimobilebanking.ui.statement

import androidx.lifecycle.*
import com.nodimobilebanking.data.models.*
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class StatementViewModel(private val repo: SaccoRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _member = MutableLiveData<Resource<MemberProfile>>()
    val member: LiveData<Resource<MemberProfile>> = _member

    private val _summary = MutableLiveData<Resource<BalanceSummary>>()
    val balanceSummary: LiveData<Resource<BalanceSummary>> = _summary

    private val _accountStatement = MutableLiveData<Resource<AccountStatement>>()
    val accountStatement: LiveData<Resource<AccountStatement>> = _accountStatement

    private val _fullStatement = MutableLiveData<Resource<FullStatementResponse>>()
    val fullStatement: LiveData<Resource<FullStatementResponse>> = _fullStatement

    private val _accounts = MutableLiveData<Resource<List<MemberAccount>>>()
    val accounts: LiveData<Resource<List<MemberAccount>>> = _accounts

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            val cD = async { repo.getMemberProfile() }
            val bD = async { repo.getBalanceSummary() }
            val cR = cD.await(); val bR = bD.await()
            _member.value = cR; _summary.value = bR
            if (cR is Resource.Error) _error.value = cR.message
            if (bR is Resource.Error) _error.value = bR.message

            // Load accounts for selector
            if (cR is Resource.Success) {
                val aR = repo.getMemberAccounts()
                _accounts.value = aR
            }

            _isLoading.value = false
        }
    }

    fun loadAccountStatement(accountType: String, fromDate: String? = null, toDate: String? = null) {
        _accountStatement.value = Resource.Loading()
        viewModelScope.launch {
            _accountStatement.value = repo.getAccountStatement(accountType, fromDate, toDate)
        }
    }

    fun loadFullStatement(fromDate: String, toDate: String) {
        _fullStatement.value = Resource.Loading()
        _isLoading.value = true
        viewModelScope.launch {
            _fullStatement.value = repo.getFullStatement(fromDate, toDate)
            _isLoading.value = false
        }
    }

    class Factory(private val repo: SaccoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(m: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return StatementViewModel(repo) as T
        }
    }
}
