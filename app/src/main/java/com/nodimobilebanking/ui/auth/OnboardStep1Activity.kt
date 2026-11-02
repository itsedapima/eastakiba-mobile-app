package com.nodimobilebanking.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.ActivityOnboardStep1Binding
import com.nodimobilebanking.utils.*
import kotlinx.coroutines.launch

/**
 * Onboarding Step 1: Member enters their member number only.
 * Backend looks up registered email + phone and sends OTP to both.
 */
class OnboardStep1Activity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardStep1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardStep1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val tm = TokenManager(applicationContext)
        val repo = SaccoRepository(RetrofitClient.getInstance(tm, applicationContext), tm)

        // Show cached company name
        lifecycleScope.launch {
            val name = try { tm.getCompanyName() } catch (_: Exception) { "" }
            if (name.isNotBlank()) binding.tvCompanyHeader.text = name.uppercase()
        }

        binding.btnSendOtp.setOnClickListener {
            val memberNo = binding.etMemberNo.text.toString().trim()
            if (memberNo.isBlank() || !memberNo.all { it.isDigit() }) {
                binding.etMemberNo.error = "Enter your member number"
                return@setOnClickListener
            }

            val loading = showLoading("Looking up your account...")
            lifecycleScope.launch {
                val result = repo.onboardInitiate(memberNo)
                loading.dismiss()
                when (result) {
                    is Resource.Success -> {
                        toast(result.data.message)
                        startActivity(
                            Intent(this@OnboardStep1Activity, OnboardStep2Activity::class.java)
                                .putExtra("member_no", memberNo)
                                .putExtra("masked_email", result.data.maskedEmail)
                                .putExtra("masked_phone", result.data.maskedPhone)
                        )
                    }
                    is Resource.Error -> {
                        if (result.message.contains("ALREADY_ACTIVATED", ignoreCase = true)
                            || result.message.contains("already activated", ignoreCase = true)) {
                            com.google.android.material.dialog.MaterialAlertDialogBuilder(this@OnboardStep1Activity)
                                .setTitle("Already Activated")
                                .setMessage("Your account is already active. Use OTP login instead.")
                                .setPositiveButton("Go to Login") { _, _ -> finish() }
                                .show()
                        } else {
                            toast(result.message)
                        }
                    }
                    else -> {}
                }
            }
        }

        binding.tvLogin.setOnClickListener { finish() }
    }
}
