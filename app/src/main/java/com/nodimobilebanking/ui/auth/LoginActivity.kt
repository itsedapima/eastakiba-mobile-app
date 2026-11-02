package com.nodimobilebanking.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nodimobilebanking.R
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.ActivityLoginBinding
import com.nodimobilebanking.ui.dashboard.MainActivity
import com.nodimobilebanking.utils.*
import kotlinx.coroutines.launch

/**
 * Login screen with TWO modes:
 *   • PIN mode (default): member number + 4-digit PIN
 *   • OTP mode: member number only → code sent → verify
 *
 * Users who don't remember their PIN can switch to OTP, or use "Forgot PIN?"
 * to reset it via OTP.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var repository: SaccoRepository

    // true = PIN mode, false = OTP mode
    private var pinMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(applicationContext)
        repository = SaccoRepository(RetrofitClient.getInstance(tokenManager, applicationContext), tokenManager)

        lifecycleScope.launch {
            val name = tokenManager.getCompanyName()
            binding.tvCompanyName.text = if (name.isNotBlank() && name != "SACCO MIS") name else getString(R.string.app_name)

            // If we remember the member number, prefill it and make it read-only.
            // The returning user only needs to enter their PIN.
            val savedMemberNo = tokenManager.getSavedMemberNo()
            if (!savedMemberNo.isNullOrBlank()) {
                binding.etMemberNo.setText(savedMemberNo)
                lockMemberField(true)
                binding.tvChangeMember.visible()
                // Focus straight to the PIN field for a faster login
                binding.etPin.requestFocus()
            } else {
                lockMemberField(false)
                binding.tvChangeMember.gone()
            }
        }
        CompanyInfoManager.refreshAsync(this)

        applyMode()

        binding.btnLogin.setOnClickListener {
            val memberNo = binding.etMemberNo.text.toString().trim()
            if (memberNo.isBlank() || !memberNo.all { it.isDigit() }) {
                binding.etMemberNo.error = "Enter your member number"; return@setOnClickListener
            }
            if (pinMode) {
                val pin = binding.etPin.text.toString().trim()
                if (pin.length != 4) { binding.etPin.error = "Enter your 4-digit PIN"; return@setOnClickListener }
                loginWithPin(memberNo, pin)
            } else {
                requestLoginOtp(memberNo)
            }
        }

        // "Not you? / Change member" — unlock the field and forget the saved number
        binding.tvChangeMember.setOnClickListener {
            lifecycleScope.launch { tokenManager.clearSavedMemberNo() }
            binding.etMemberNo.setText("")
            lockMemberField(false)
            binding.tvChangeMember.gone()
            binding.etMemberNo.requestFocus()
        }

        binding.tvToggleMode.setOnClickListener {
            pinMode = !pinMode
            applyMode()
        }

        binding.tvForgotPin.setOnClickListener {
            val memberNo = binding.etMemberNo.text.toString().trim()
            showForgotPinDialog(memberNo)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, OnboardStep1Activity::class.java))
        }
    }

    private fun lockMemberField(locked: Boolean) {
        binding.etMemberNo.isEnabled = !locked
        binding.etMemberNo.isFocusable = !locked
        binding.etMemberNo.isFocusableInTouchMode = !locked
        binding.tilMemberNo.isEnabled = !locked
    }

    private fun applyMode() {
        if (pinMode) {
            binding.tvLoginTitle.text = "Sign in with PIN"
            binding.tilPin.visible()
            binding.tvForgotPin.visible()
            binding.btnLogin.text = "Login"
            binding.tvToggleMode.text = "🔐 Login with OTP instead"
        } else {
            binding.tvLoginTitle.text = "Sign in with OTP"
            binding.tilPin.gone()
            binding.tvForgotPin.gone()
            binding.btnLogin.text = "Send Login Code"
            binding.tvToggleMode.text = "🔢 Login with PIN instead"
        }
    }

    // ── PIN LOGIN ────────────────────────────────────────────────────────────
    private fun loginWithPin(memberNo: String, pin: String) {
        val loading = showLoading("Signing in...")
        lifecycleScope.launch {
            val result = repository.loginWithPin(memberNo, pin)
            loading.dismiss()
            when (result) {
                is Resource.Success -> goToDashboard()
                is Resource.Error -> {
                    if (result.message.contains("NOT_ONBOARDED", true)
                        || result.message.contains("not activated", true)) {
                        toast("Please activate your account first")
                        startActivity(Intent(this@LoginActivity, OnboardStep1Activity::class.java))
                    } else {
                        toast(result.message)
                    }
                }
                else -> {}
            }
        }
    }

    // ── OTP LOGIN ──────────────────────────────────────────────────────────────
    private fun requestLoginOtp(memberNo: String) {
        val loading = showLoading("Sending login code...")
        lifecycleScope.launch {
            val result = repository.loginRequestOtp(memberNo)
            loading.dismiss()
            when (result) {
                is Resource.Success -> {
                    val targets = listOfNotNull(
                        result.data.maskedEmail.takeIf { it.isNotBlank() },
                        result.data.maskedPhone.takeIf { it.isNotBlank() }
                    ).joinToString(" and ")
                    showOtpDialog(memberNo, "Login code sent to $targets")
                }
                is Resource.Error -> {
                    if (result.message.contains("NOT_ONBOARDED", true)
                        || result.message.contains("not activated", true)) {
                        toast("Please activate your account first")
                        startActivity(Intent(this@LoginActivity, OnboardStep1Activity::class.java))
                    } else {
                        toast(result.message)
                    }
                }
                else -> {}
            }
        }
    }

    private fun showOtpDialog(memberNo: String, message: String) {
        val etOtp = DialogInputs.input(
            this, "6-digit code", InputType.TYPE_CLASS_NUMBER,
            centerText = true, large = true
        )
        etOtp.filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        MaterialAlertDialogBuilder(this)
            .setTitle("Enter Verification Code")
            .setMessage(message)
            .setView(DialogInputs.wrap(this, etOtp))
            .setPositiveButton("Verify") { _, _ ->
                val otp = etOtp.text.toString().trim()
                if (otp.length != 6) { toast("Enter 6-digit code"); return@setPositiveButton }
                verifyLoginOtp(memberNo, otp)
            }
            .setNeutralButton("Resend Code") { _, _ -> requestLoginOtp(memberNo) }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun verifyLoginOtp(memberNo: String, otp: String) {
        val loading = showLoading("Verifying...")
        lifecycleScope.launch {
            val result = repository.loginVerifyOtp(memberNo, otp)
            loading.dismiss()
            when (result) {
                is Resource.Success -> goToDashboard()
                is Resource.Error -> toast(result.message)
                else -> {}
            }
        }
    }

    // ── FORGOT PIN (3-step OTP reset) ───────────────────────────────────────────
    private fun showForgotPinDialog(prefillMemberNo: String) {
        val etMember = DialogInputs.input(
            this, "Member Number", InputType.TYPE_CLASS_NUMBER
        )
        if (prefillMemberNo.isNotBlank()) etMember.setText(prefillMemberNo)
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset PIN — Step 1 of 3")
            .setMessage("Enter your member number. We'll send a reset code to your registered email and phone.")
            .setView(DialogInputs.wrap(this, etMember))
            .setPositiveButton("Send Code") { _, _ ->
                val memberNo = etMember.text.toString().trim()
                if (memberNo.isBlank() || !memberNo.all { it.isDigit() }) {
                    toast("Enter a valid member number"); return@setPositiveButton
                }
                requestPinReset(memberNo)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestPinReset(memberNo: String) {
        val loading = showLoading("Sending reset code...")
        lifecycleScope.launch {
            val result = repository.passwordResetRequest(memberNo)
            loading.dismiss()
            when (result) {
                is Resource.Success -> {
                    val targets = listOfNotNull(
                        result.data.maskedEmail.takeIf { it.isNotBlank() },
                        result.data.maskedPhone.takeIf { it.isNotBlank() }
                    ).joinToString(" and ")
                    showPinResetOtpDialog(memberNo, targets)
                }
                is Resource.Error -> toast(result.message)
                else -> {}
            }
        }
    }

    private fun showPinResetOtpDialog(memberNo: String, targets: String) {
        val etOtp = DialogInputs.input(
            this, "6-digit code", InputType.TYPE_CLASS_NUMBER,
            centerText = true, large = true
        )
        etOtp.filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        val msg = if (targets.isNotBlank()) "Code sent to $targets." else "Code sent."
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset PIN — Step 2 of 3")
            .setMessage(msg)
            .setView(DialogInputs.wrap(this, etOtp))
            .setPositiveButton("Verify") { _, _ ->
                val otp = etOtp.text.toString().trim()
                if (otp.length != 6) { toast("Enter 6-digit code"); return@setPositiveButton }
                verifyPinResetOtp(memberNo, otp)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun verifyPinResetOtp(memberNo: String, otp: String) {
        val loading = showLoading("Verifying...")
        lifecycleScope.launch {
            val result = repository.passwordResetVerifyOtp(memberNo, otp)
            loading.dismiss()
            when (result) {
                is Resource.Success -> showNewPinDialog(result.data.resetToken)
                is Resource.Error -> toast(result.message)
                else -> {}
            }
        }
    }

    private fun showNewPinDialog(resetToken: String) {
        val etNew = DialogInputs.input(
            this, "New 4-digit PIN", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            centerText = true, large = true
        )
        etNew.filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        val etConfirm = DialogInputs.input(
            this, "Confirm PIN", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
            centerText = true, large = true
        )
        etConfirm.filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset PIN — Step 3 of 3")
            .setMessage("Set your new 4-digit PIN.")
            .setView(DialogInputs.stack(this, etNew, etConfirm))
            .setPositiveButton("Save PIN") { _, _ ->
                val pin = etNew.text.toString().trim()
                val confirm = etConfirm.text.toString().trim()
                if (pin.length != 4) { toast("PIN must be 4 digits"); return@setPositiveButton }
                if (pin != confirm) { toast("PINs don't match"); return@setPositiveButton }
                confirmPinReset(resetToken, pin, confirm)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun confirmPinReset(resetToken: String, pin: String, confirm: String) {
        val loading = showLoading("Saving PIN...")
        lifecycleScope.launch {
            val result = repository.passwordResetConfirm(resetToken, pin, confirm)
            loading.dismiss()
            when (result) {
                is Resource.Success -> {
                    toast("PIN reset. Sign in with your new PIN.")
                    pinMode = true; applyMode()
                }
                is Resource.Error -> toast(result.message)
                else -> {}
            }
        }
    }

    private fun goToDashboard() {
        toast("Welcome!")
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity()
    }
}
