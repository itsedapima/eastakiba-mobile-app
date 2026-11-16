package com.nodimobilebanking.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nodimobilebanking.data.models.MemberCategory
import com.nodimobilebanking.data.models.SelfRegCompleteResponse
import com.nodimobilebanking.data.models.SelfRegPaymentResponse
import com.nodimobilebanking.data.models.SelfRegValidateResponse
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.utils.Resource
import kotlinx.coroutines.launch
import java.io.File

class SelfRegisterViewModel(private val repository: SaccoRepository) : ViewModel() {

    // ── Step 1: Personal Details ─────────────────────────────────────────────
    var firstName = ""
    var middleName = ""
    var lastName = ""
    var gender = ""
    var dob = ""
    var memberType = ""
    var maritalStatus = "Un-known"

    // ── Step 2: Contact & Identity ───────────────────────────────────────────
    var phone = ""
    var email = ""
    var nationalId = ""
    var kraPin = ""
    var town = ""
    var postalAddress = ""

    // ── Step 3: Documents ────────────────────────────────────────────────────
    var passportPhotoUri: Uri? = null
    var idFrontUri: Uri? = null
    var idBackUri: Uri? = null

    // ── Step 4: Next of Kin ──────────────────────────────────────────────────
    var kinName = ""
    var kinRelationship = "Other"
    var kinPhone = ""
    var kinNationalId = ""

    // ── Registration state ───────────────────────────────────────────────────
    var registrationToken = ""
    var feeAmount = 0.0

    // ── Categories ───────────────────────────────────────────────────────────
    private val _categories = MutableLiveData<Resource<List<MemberCategory>>>()
    val categories: LiveData<Resource<List<MemberCategory>>> = _categories

    private val _validateResult = MutableLiveData<Resource<SelfRegValidateResponse>>()
    val validateResult: LiveData<Resource<SelfRegValidateResponse>> = _validateResult

    private val _uploadResult = MutableLiveData<Resource<String>>()
    val uploadResult: LiveData<Resource<String>> = _uploadResult

    private val _paymentResult = MutableLiveData<Resource<SelfRegPaymentResponse>>()
    val paymentResult: LiveData<Resource<SelfRegPaymentResponse>> = _paymentResult

    private val _verifyResult = MutableLiveData<Resource<SelfRegCompleteResponse>>()
    val verifyResult: LiveData<Resource<SelfRegCompleteResponse>> = _verifyResult

    var uploadsPending = 0
    var uploadsCompleted = 0

    fun loadCategories() {
        viewModelScope.launch {
            _categories.value = Resource.Loading()
            _categories.value = repository.getMemberCategories()
        }
    }

    fun getFeeForCategory(category: String): Double {
        val cats = (categories.value as? Resource.Success)?.data ?: return 0.0
        return cats.find { it.category == category }?.amount ?: 0.0
    }

    fun validateRegistration() {
        viewModelScope.launch {
            _validateResult.value = Resource.Loading()
            val data = mutableMapOf(
                "first_name" to firstName.trim(),
                "last_name" to lastName.trim(),
                "gender" to gender,
                "dob" to dob,
                "member_type" to memberType,
                "phone" to phone.trim(),
                "email" to email.trim(),
                "national_id" to nationalId.trim(),
                "kin_name" to kinName.trim(),
                "kin_relationship" to kinRelationship,
            )
            if (middleName.isNotBlank()) data["middle_name"] = middleName.trim()
            if (kraPin.isNotBlank()) data["kra_pin"] = kraPin.trim()
            if (town.isNotBlank()) data["town"] = town.trim()
            if (postalAddress.isNotBlank()) data["postal_address"] = postalAddress.trim()
            if (maritalStatus.isNotBlank()) data["marital_status"] = maritalStatus
            if (kinPhone.isNotBlank()) data["kin_phone"] = kinPhone.trim()
            if (kinNationalId.isNotBlank()) data["kin_national_id"] = kinNationalId.trim()

            val result = repository.selfRegValidate(data)
            if (result is Resource.Success) {
                registrationToken = result.data!!.registrationToken
                feeAmount = result.data.feeAmount
            }
            _validateResult.value = result
        }
    }

    fun uploadDocument(docType: String, file: File) {
        viewModelScope.launch {
            _uploadResult.value = Resource.Loading()
            val result = repository.selfRegUploadDocument(registrationToken, docType, file)
            if (result is Resource.Success) uploadsCompleted++
            _uploadResult.value = result
        }
    }

    fun initiatePayment() {
        viewModelScope.launch {
            _paymentResult.value = Resource.Loading()
            _paymentResult.value = repository.selfRegInitiatePayment(registrationToken)
        }
    }

    fun verifyPayment() {
        viewModelScope.launch {
            _verifyResult.value = Resource.Loading()
            _verifyResult.value = repository.selfRegVerifyPayment(registrationToken)
        }
    }
}
