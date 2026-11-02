package com.nodimobilebanking.ui.transactions

import androidx.lifecycle.*
import com.nodimobilebanking.data.models.*
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class TransactionsViewModel(private val repo: SaccoRepository) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _savings = MutableLiveData<Resource<List<SavingsTransaction>>>()
    val savingsTransactions: LiveData<Resource<List<SavingsTransaction>>> = _savings

    private val _loans = MutableLiveData<Resource<List<LoanTransaction>>>()
    val loanTransactions: LiveData<Resource<List<LoanTransaction>>> = _loans

    fun loadAll() {
        _isLoading.value = true
        viewModelScope.launch {
            val sD = async { repo.getSavingsTransactions() }
            val lD = async { repo.getLoanTransactions() }
            _savings.value = sD.await()
            _loans.value   = lD.await()
            _isLoading.value = false
        }
    }

    class Factory(private val repo: SaccoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(m: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return TransactionsViewModel(repo) as T
        }
    }
}
