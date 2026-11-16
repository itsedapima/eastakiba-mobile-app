package com.nodimobilebanking.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nodimobilebanking.databinding.FragmentSelfregStep5Binding
import com.nodimobilebanking.utils.Resource
import com.nodimobilebanking.utils.showLoading
import com.nodimobilebanking.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

class SelfRegStep5Fragment : Fragment() {

    private var _binding: FragmentSelfregStep5Binding? = null
    private val binding get() = _binding!!
    private val vm get() = (requireActivity() as SelfRegisterActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSelfregStep5Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateSummary()
        observeResults()
    }

    private fun populateSummary() {
        binding.tvSummaryPersonal.text = buildString {
            appendLine("Name: ${vm.firstName} ${vm.middleName} ${vm.lastName}".trim())
            appendLine("Gender: ${vm.gender}")
            appendLine("DOB: ${vm.dob}")
            appendLine("Category: ${vm.memberType}")
            if (vm.maritalStatus.isNotBlank()) appendLine("Marital Status: ${vm.maritalStatus}")
        }

        binding.tvSummaryContact.text = buildString {
            appendLine("Phone: ${vm.phone}")
            appendLine("Email: ${vm.email}")
            appendLine("National ID: ${vm.nationalId}")
            if (vm.kraPin.isNotBlank()) appendLine("KRA PIN: ${vm.kraPin}")
            if (vm.town.isNotBlank()) appendLine("Town: ${vm.town}")
            if (vm.postalAddress.isNotBlank()) appendLine("Postal Address: ${vm.postalAddress}")
        }

        binding.tvSummaryKin.text = buildString {
            appendLine("Name: ${vm.kinName}")
            appendLine("Relationship: ${vm.kinRelationship}")
            if (vm.kinPhone.isNotBlank()) appendLine("Phone: ${vm.kinPhone}")
            if (vm.kinNationalId.isNotBlank()) appendLine("ID: ${vm.kinNationalId}")
        }

        val docsCount = listOfNotNull(vm.passportPhotoUri, vm.idFrontUri, vm.idBackUri).size
        binding.tvSummaryDocs.text = "$docsCount of 3 documents uploaded"

        val fee = vm.getFeeForCategory(vm.memberType)
        binding.tvFeeAmount.text = if (fee > 0) "KES ${String.format("%,.0f", fee)}" else "FREE"
    }

    private fun observeResults() {
        vm.validateResult.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> uploadDocuments()
                is Resource.Error -> {
                    setStatus(res.message ?: "Validation failed")
                    enableButtons(true)
                }
                is Resource.Loading -> setStatus("Validating your details...")
            }
        }

        vm.paymentResult.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    val data = res.data!!
                    if (data.skipPayment) {
                        setStatus("No fee required. Completing registration...")
                        vm.verifyPayment()
                    } else {
                        setStatus("M-Pesa prompt sent to your phone. Enter your PIN to pay, then wait...")
                        startPaymentPolling()
                    }
                }
                is Resource.Error -> {
                    setStatus(res.message ?: "Payment initiation failed")
                    enableButtons(true)
                }
                is Resource.Loading -> setStatus("Sending M-Pesa payment prompt...")
            }
        }

        vm.verifyResult.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    val data = res.data!!
                    enableButtons(true)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Registration Successful!")
                        .setMessage("Welcome! Your member number is ${data.memberNo}.\n\nYou'll now set up your PIN to secure your account.")
                        .setCancelable(false)
                        .setPositiveButton("Set PIN") { _, _ ->
                            startActivity(Intent(requireContext(), OnboardStep3Activity::class.java).apply {
                                putExtra("member_no", data.memberNo)
                                putExtra("from_self_register", true)
                            })
                            requireActivity().finish()
                        }
                        .show()
                }
                is Resource.Error -> {
                    setStatus(res.message ?: "Verification failed")
                    enableButtons(true)
                }
                is Resource.Loading -> setStatus("Verifying payment...")
            }
        }
    }

    fun startSubmission() {
        enableButtons(false)
        vm.validateRegistration()
    }

    private fun uploadDocuments() {
        setStatus("Uploading documents...")
        lifecycleScope.launch {
            val docs = listOf(
                "passport_photo" to vm.passportPhotoUri,
                "id_front" to vm.idFrontUri,
                "id_back" to vm.idBackUri,
            )
            for ((type, uri) in docs) {
                if (uri == null) continue
                setStatus("Uploading ${type.replace("_", " ")}...")
                val file = uriToFile(uri, type) ?: run {
                    setStatus("Failed to prepare $type image")
                    enableButtons(true)
                    return@launch
                }
                val result = (requireActivity() as SelfRegisterActivity).viewModel
                    .let { viewModel ->
                        val repo = com.nodimobilebanking.data.repository.SaccoRepository(
                            com.nodimobilebanking.data.api.RetrofitClient.getPublicInstance(requireContext()),
                            com.nodimobilebanking.utils.TokenManager(requireContext())
                        )
                        repo.selfRegUploadDocument(viewModel.registrationToken, type, file)
                    }
                if (result is Resource.Error) {
                    setStatus(result.message ?: "Upload failed")
                    enableButtons(true)
                    return@launch
                }
            }
            setStatus("Documents uploaded. Initiating payment...")
            vm.initiatePayment()
        }
    }

    private fun startPaymentPolling() {
        lifecycleScope.launch {
            repeat(12) {
                delay(5000)
                if (_binding == null) return@launch
                val currentResult = vm.verifyResult.value
                if (currentResult is Resource.Success) return@launch
                vm.verifyPayment()
                val res = vm.verifyResult.value
                if (res is Resource.Success) return@launch
                if (res is Resource.Error && !(res.message ?: "").contains("not yet received", ignoreCase = true)) {
                    return@launch
                }
                setStatus("Waiting for payment... (${it + 1}/12)")
            }
            if (_binding != null) {
                setStatus("Payment not received yet. Tap 'Submit & Pay' to retry.")
                enableButtons(true)
            }
        }
    }

    private fun uriToFile(uri: Uri, name: String): File? {
        return try {
            val mime = requireContext().contentResolver.getType(uri)
            val ext = when {
                mime?.contains("png") == true -> ".png"
                mime?.contains("webp") == true -> ".webp"
                else -> ".jpg"
            }
            val file = File(requireContext().cacheDir, "selfreg_${name}_${System.currentTimeMillis()}$ext")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            if (file.length() > 15 * 1024 * 1024) {
                requireContext().toast("Image too large (max 15MB)")
                null
            } else file
        } catch (e: Exception) {
            null
        }
    }

    private fun setStatus(text: String) {
        binding.tvPaymentStatus.visibility = View.VISIBLE
        binding.tvPaymentStatus.text = text
    }

    private fun enableButtons(enabled: Boolean) {
        (requireActivity() as? SelfRegisterActivity)?.setBottomButtonsEnabled(enabled)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
