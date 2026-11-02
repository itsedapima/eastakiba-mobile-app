package com.nodimobilebanking.ui.profile

import androidx.lifecycle.*
import com.nodimobilebanking.data.models.MemberProfile
import com.nodimobilebanking.data.models.GuarantorEntry
import com.nodimobilebanking.data.models.NextOfKin
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.utils.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ProfileViewModel(private val repo: SaccoRepository) : ViewModel() {
    private val _profile = MutableLiveData<Resource<MemberProfile>>()
    val profile: LiveData<Resource<MemberProfile>> = _profile

    private val _kins = MutableLiveData<Resource<List<NextOfKin>>>()
    val kins: LiveData<Resource<List<NextOfKin>>> = _kins

    private val _myGuarantors = MutableLiveData<Resource<List<GuarantorEntry>>>()
    val myGuarantors: LiveData<Resource<List<GuarantorEntry>>> = _myGuarantors

    private val _myGuarantees = MutableLiveData<Resource<List<GuarantorEntry>>>()
    val myGuarantees: LiveData<Resource<List<GuarantorEntry>>> = _myGuarantees

    fun loadProfile() {
        _profile.value = Resource.Loading()
        _kins.value = Resource.Loading()
        _myGuarantors.value = Resource.Loading()
        _myGuarantees.value = Resource.Loading()
        viewModelScope.launch {
            val profileD = async { repo.getMemberProfile() }
            val kinsD = async { repo.getNextOfKins() }
            val guarantorsD = async { repo.getMyGuarantors() }
            val guaranteesD = async { repo.getMyGuarantees() }
            _profile.value = profileD.await()
            _kins.value = kinsD.await()
            _myGuarantors.value = guarantorsD.await()
            _myGuarantees.value = guaranteesD.await()
        }
    }

    class Factory(private val repo: SaccoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(m: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return ProfileViewModel(repo) as T
        }
    }
}
