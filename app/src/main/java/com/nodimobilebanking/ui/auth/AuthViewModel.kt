package com.nodimobilebanking.ui.auth

import androidx.lifecycle.*
import com.nodimobilebanking.data.models.*
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.utils.Resource
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: SaccoRepository) : ViewModel() {

    // ── Onboarding Step 1: initiate (member_no only) ──────────────────────────
    private val _initiateState = MutableLiveData<Resource<OtpSentResponse>>()
    val initiateState: LiveData<Resource<OtpSentResponse>> = _initiateState

    fun onboardInitiate(memberNo: String) {
        _initiateState.value = Resource.Loading()
        viewModelScope.launch { _initiateState.value = repository.onboardInitiate(memberNo) }
    }

    // ── Onboarding Step 2: verify OTP → account activated, JWT returned ──────
    private val _verifyOtpState = MutableLiveData<Resource<LoginResponse>>()
    val verifyOtpState: LiveData<Resource<LoginResponse>> = _verifyOtpState

    fun onboardVerifyOtp(memberNo: String, otp: String) {
        _verifyOtpState.value = Resource.Loading()
        viewModelScope.launch { _verifyOtpState.value = repository.onboardVerifyOtp(memberNo, otp) }
    }

    // ── Onboarding: resend OTP ───────────────────────────────────────────────
    private val _resendState = MutableLiveData<Resource<String>>()
    val resendState: LiveData<Resource<String>> = _resendState

    fun onboardResendOtp(memberNo: String) {
        _resendState.value = Resource.Loading()
        viewModelScope.launch { _resendState.value = repository.onboardResendOtp(memberNo) }
    }

    // ── Onboarding Step 3: set 4-digit PIN (authenticated) ───────────────────
    private val _setPinState = MutableLiveData<Resource<String>>()
    val setPinState: LiveData<Resource<String>> = _setPinState

    fun setPin(pin: String) {
        _setPinState.value = Resource.Loading()
        viewModelScope.launch { _setPinState.value = repository.setPin(pin) }
    }

    class Factory(private val repository: SaccoRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST") return AuthViewModel(repository) as T
        }
    }
}
