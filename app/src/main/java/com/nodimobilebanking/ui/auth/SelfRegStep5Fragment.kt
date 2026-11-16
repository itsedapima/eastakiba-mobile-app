package com.nodimobilebanking.ui.auth

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nodimobilebanking.R
import com.nodimobilebanking.databinding.FragmentSelfregStep5Binding
import com.nodimobilebanking.utils.Resource
import com.nodimobilebanking.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class SelfRegStep5Fragment : Fragment() {

    private var _binding: FragmentSelfregStep5Binding? = null
    private val binding get() = _binding!!
    private val vm get() = (requireActivity() as SelfRegisterActivity).viewModel

    private var progressDialog: Dialog? = null
    private var progressMessage: TextView? = null
    private var isProcessing = false

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
                    dismissProgress()
                    isProcessing = false
                    enableButtons(true)
                    requireContext().toast(res.message ?: "Validation failed")
                }
                is Resource.Loading -> showProgress("Validating your details...")
            }
        }

        vm.paymentResult.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    val data = res.data!!
                    if (data.skipPayment) {
                        showProgress("No fee required. Completing registration...")
                        vm.verifyPayment()
                    } else {
                        showProgress("M-Pesa prompt sent to ${vm.phone}.\nEnter your M-Pesa PIN to pay...")
                        startPaymentPolling()
                    }
                }
                is Resource.Error -> {
                    dismissProgress()
                    isProcessing = false
                    enableButtons(true)
                    requireContext().toast(res.message ?: "Payment initiation failed")
                }
                is Resource.Loading -> showProgress("Sending M-Pesa payment prompt...")
            }
        }

        vm.verifyResult.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    dismissProgress()
                    isProcessing = false
                    val data = res.data!!
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
                    if ((res.message ?: "").contains("not yet received", ignoreCase = true)) return@observe
                    dismissProgress()
                    isProcessing = false
                    enableButtons(true)
                    requireContext().toast(res.message ?: "Verification failed")
                }
                is Resource.Loading -> showProgress("Verifying payment...")
            }
        }
    }

    fun startSubmission() {
        if (isProcessing) return
        isProcessing = true
        enableButtons(false)
        vm.validateRegistration()
    }

    private fun uploadDocuments() {
        showProgress("Uploading documents...")
        lifecycleScope.launch {
            val docs = listOf(
                "passport_photo" to vm.passportPhotoUri,
                "id_front" to vm.idFrontUri,
                "id_back" to vm.idBackUri,
            )
            for ((type, uri) in docs) {
                if (uri == null) continue
                val label = type.replace("_", " ")
                showProgress("Uploading $label...")
                val file = uriToFile(uri, type) ?: run {
                    dismissProgress()
                    isProcessing = false
                    enableButtons(true)
                    requireContext().toast("Failed to prepare $label image")
                    return@launch
                }
                val result = vm.let { viewModel ->
                    val repo = com.nodimobilebanking.data.repository.SaccoRepository(
                        com.nodimobilebanking.data.api.RetrofitClient.getPublicInstance(requireContext()),
                        com.nodimobilebanking.utils.TokenManager(requireContext())
                    )
                    repo.selfRegUploadDocument(viewModel.registrationToken, type, file)
                }
                if (result is Resource.Error) {
                    dismissProgress()
                    isProcessing = false
                    enableButtons(true)
                    requireContext().toast(result.message ?: "Upload failed for $label")
                    return@launch
                }
            }
            showProgress("Documents uploaded. Initiating payment...")
            vm.initiatePayment()
        }
    }

    private fun startPaymentPolling() {
        lifecycleScope.launch {
            repeat(12) { attempt ->
                delay(5000)
                if (_binding == null) return@launch
                val currentResult = vm.verifyResult.value
                if (currentResult is Resource.Success) return@launch
                showProgress("Waiting for M-Pesa payment confirmation...\nAttempt ${attempt + 1} of 12")
                vm.verifyPayment()
                val res = vm.verifyResult.value
                if (res is Resource.Success) return@launch
                if (res is Resource.Error && !(res.message ?: "").contains("not yet received", ignoreCase = true)) {
                    return@launch
                }
            }
            if (_binding != null) {
                dismissProgress()
                isProcessing = false
                enableButtons(true)
                requireContext().toast("Payment not received yet. Please try again.")
            }
        }
    }

    private fun showProgress(message: String) {
        if (_binding == null) return
        requireActivity().runOnUiThread {
            if (progressDialog == null || !progressDialog!!.isShowing) {
                progressDialog = Dialog(requireContext()).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)

                    val container = LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                        setPadding(64, 48, 64, 48)
                        setBackgroundResource(R.drawable.bg_progress_dialog)
                    }

                    val spinner = ProgressBar(context).apply {
                        isIndeterminate = true
                        val size = (48 * resources.displayMetrics.density).toInt()
                        layoutParams = LinearLayout.LayoutParams(size, size)
                    }
                    container.addView(spinner)

                    val tv = TextView(context).apply {
                        text = message
                        textSize = 14f
                        setTextColor(context.getColor(R.color.text_primary))
                        gravity = Gravity.CENTER
                        val topMargin = (16 * resources.displayMetrics.density).toInt()
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { setMargins(0, topMargin, 0, 0) }
                    }
                    container.addView(tv)
                    progressMessage = tv

                    setContentView(container)
                    window?.apply {
                        setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        setLayout(
                            (280 * resources.displayMetrics.density).toInt(),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setGravity(Gravity.CENTER)
                    }
                    show()
                }
            } else {
                progressMessage?.text = message
            }
        }
    }

    private fun dismissProgress() {
        progressDialog?.dismiss()
        progressDialog = null
        progressMessage = null
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
            if (file.length() > 5 * 1024 * 1024) {
                requireContext().toast("Image too large (max 5MB)")
                null
            } else file
        } catch (e: Exception) {
            null
        }
    }

    private fun enableButtons(enabled: Boolean) {
        (requireActivity() as? SelfRegisterActivity)?.setBottomButtonsEnabled(enabled)
    }

    override fun onDestroyView() {
        dismissProgress()
        super.onDestroyView()
        _binding = null
    }
}
