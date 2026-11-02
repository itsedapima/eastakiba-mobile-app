package com.nodimobilebanking.ui.accounts

import androidx.lifecycle.*
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.utils.Resource
import kotlinx.coroutines.launch

class AccountsViewModel(private val repo: SaccoRepository) : ViewModel() {

    private val _state = MutableLiveData<Resource<List<AccountItem>>>()
    val state: LiveData<Resource<List<AccountItem>>> = _state

    fun load() {
        _state.value = Resource.Loading()
        viewModelScope.launch {
            try {
                val memberResult = repo.getMemberProfile()
                if (memberResult is Resource.Error) {
                    _state.value = Resource.Error(memberResult.message)
                    return@launch
                }
                val member = (memberResult as Resource.Success).data

                val accountsResult = repo.getMemberAccounts()
                if (accountsResult is Resource.Error) {
                    _state.value = Resource.Error(accountsResult.message)
                    return@launch
                }
                val accounts = (accountsResult as Resource.Success).data

                val items = accounts.map { acc ->
                    val rawSub = (acc.subText ?: "").ifBlank { acc.id }
                    val cleanCode = if (rawSub.contains("(") && rawSub.contains(")")) {
                        rawSub.substringAfter("(").substringBefore(")")
                    } else {
                        acc.accountCode?.takeIf { it.isNotBlank() } ?: rawSub
                    }
                    AccountItem(
                        accountCode = acc.id,
                        accountName = (acc.accountName ?: "").ifBlank {
                            (acc.text ?: "").substringAfter("-", acc.text ?: "").substringBefore("(").trim()
                        },
                        subText = cleanCode,
                        balance = acc.balance,
                        isLoan = acc.isLoan,
                        loanNo = if (acc.isLoan) acc.id else null
                    )
                }

                val nonZero = items.filter { it.balance != 0.0 }
                _state.value = Resource.Success(nonZero)
            } catch (e: Exception) {
                _state.value = Resource.Error("Failed to load accounts: ${e.localizedMessage}")
            }
        }
    }

    class Factory(private val repo: SaccoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(m: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return AccountsViewModel(repo) as T
        }
    }
}
