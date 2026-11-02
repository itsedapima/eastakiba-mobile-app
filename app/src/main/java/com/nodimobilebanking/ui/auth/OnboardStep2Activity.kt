package com.nodimobilebanking.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.ActivityOnboardStep2Binding
import com.nodimobilebanking.utils.*
import kotlinx.coroutines.launch

/**
 * Onboarding Step 2 (FINAL): Verify OTP → account created → JWT returned → dashboard.
 * No password needed. Backend creates account with random internal password.
 */
class OnboardStep2Activity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardStep2Binding
    private lateinit var repository: SaccoRepository
    private var countDownTimer: CountDownTimer? = null
    private lateinit var memberNo: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardStep2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        memberNo = intent.getStringExtra("member_no") ?: ""
        val maskedEmail = intent.getStringExtra("masked_email") ?: ""
        val maskedPhone = intent.getStringExtra("masked_phone") ?: ""

        val tm = TokenManager(applicationContext)
        repository = SaccoRepository(RetrofitClient.getInstance(tm, applicationContext), tm)

        // Show cached company name
        lifecycleScope.launch {
            val name = try { tm.getCompanyName() } catch (_: Exception) { "" }
            if (name.isNotBlank()) binding.tvCompanyHeader.text = name.uppercase()
        }

        // Show where the code was sent
        val targets = listOfNotNull(
            maskedEmail.takeIf { it.isNotBlank() },
            maskedPhone.takeIf { it.isNotBlank() }
        ).joinToString(" and ")
        binding.tvEmailHint.text = "We sent a 6-digit code to $targets"

        startResendTimer()

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString().trim()
            if (otp.length != 6) { binding.etOtp.error = "Enter 6-digit code"; return@setOnClickListener }
            verifyAndActivate(otp)
        }

        binding.tvResend.setOnClickListener {
            binding.tvResend.isEnabled = false
            startResendTimer()
            lifecycleScope.launch {
                val result = repository.onboardResendOtp(memberNo)
                when (result) {
                    is Resource.Success -> toast("New code sent")
                    is Resource.Error -> toast(result.message)
                    else -> {}
                }
            }
        }
    }

    private fun verifyAndActivate(otp: String) {
        val loading = showLoading("Activating your account...")
        lifecycleScope.launch {
            val result = repository.onboardVerifyOtp(memberNo, otp)
            loading.dismiss()
            when (result) {
                is Resource.Success -> {
                    toast("Account activated! Now set your login PIN.")
                    startActivity(
                        Intent(this@OnboardStep2Activity, OnboardStep3Activity::class.java)
                            .putExtra("member_no", memberNo)
                    )
                    finish()
                }
                is Resource.Error -> toast(result.message)
                else -> {}
            }
        }
    }

    private fun startResendTimer() {
        countDownTimer?.cancel()
        binding.tvResend.isEnabled = false
        countDownTimer = object : CountDownTimer(60_000, 1000) {
            override fun onTick(ms: Long) {
                binding.tvResend.text = "Resend Code (${ms / 1000}s)"
            }
            override fun onFinish() {
                binding.tvResend.isEnabled = true
                binding.tvResend.text = "Resend Code"
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
