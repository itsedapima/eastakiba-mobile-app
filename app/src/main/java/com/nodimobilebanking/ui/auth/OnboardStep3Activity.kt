package com.nodimobilebanking.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nodimobilebanking.R
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.ActivityOnboardStep3Binding
import com.nodimobilebanking.ui.dashboard.MainActivity
import com.nodimobilebanking.utils.*
import kotlinx.coroutines.launch

/**
 * Onboarding Step 3 (final): Set 4-digit login PIN.
 * User is already authenticated (tokens saved in Step 2).
 */
class OnboardStep3Activity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardStep3Binding
    private lateinit var repository: SaccoRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardStep3Binding.inflate(layoutInflater)
        setContentView(binding.root)

        val tm = TokenManager(applicationContext)
        repository = SaccoRepository(RetrofitClient.getInstance(tm, applicationContext), tm)

        // Show cached company name
        lifecycleScope.launch {
            val name = try { tm.getCompanyName() } catch (_: Exception) { "" }
            if (name.isNotBlank()) binding.tvCompanyHeader.text = name.uppercase()
        }

        // Privacy policy checkbox gates the button
        binding.btnSetPin.isEnabled = false
        binding.cbPrivacyPolicy.setOnCheckedChangeListener { _, isChecked ->
            binding.btnSetPin.isEnabled = isChecked
        }

        // Tappable privacy policy text
        val label = "I agree to the Privacy Policy and Terms of Service"
        val spannable = SpannableString(label).apply {
            setSpan(UnderlineSpan(), label.indexOf("Privacy"), label.length, 0)
        }
        binding.tvPrivacyLink.text = spannable
        binding.tvPrivacyLink.setOnClickListener { showPrivacyPolicy() }

        // Set PIN
        binding.btnSetPin.setOnClickListener {
            val pin = binding.etPin.text.toString().trim()
            val confirm = binding.etPinConfirm.text.toString().trim()

            if (pin.length != 4) {
                binding.etPin.error = "PIN must be exactly 4 digits"
                return@setOnClickListener
            }
            if (pin != confirm) {
                binding.etPinConfirm.error = "PINs do not match"
                return@setOnClickListener
            }
            if (!binding.cbPrivacyPolicy.isChecked) {
                toast("Please accept the Privacy Policy")
                return@setOnClickListener
            }
            setPin(pin)
        }

        // Skip PIN — user will rely on OTP login. Privacy policy still required.
        binding.tvSkipPin.setOnClickListener {
            if (!binding.cbPrivacyPolicy.isChecked) {
                toast("Please accept the Privacy Policy to continue")
                return@setOnClickListener
            }
            toast("You can set a PIN later from Settings.")
            startActivity(Intent(this@OnboardStep3Activity, MainActivity::class.java))
            finishAffinity()
        }
    }

    private fun setPin(pin: String) {
        val loading = showLoading("Setting your PIN...")
        lifecycleScope.launch {
            val result = repository.setPin(pin)
            loading.dismiss()
            when (result) {
                is Resource.Success -> {
                    toast("All set! Welcome to Nodi.")
                    startActivity(Intent(this@OnboardStep3Activity, MainActivity::class.java))
                    finishAffinity()
                }
                is Resource.Error -> toast(result.message)
                else -> {}
            }
        }
    }

    private fun showPrivacyPolicy() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(getColor(R.color.dialog_bg))
        }
        val tv = TextView(this).apply {
            setPadding(48, 32, 48, 32)
            textSize = 13f
            setTextColor(getColor(R.color.text_primary))
            text = PRIVACY_POLICY_TEXT
        }
        scrollView.addView(tv)
        MaterialAlertDialogBuilder(this)
            .setTitle("Privacy Policy & Terms of Service")
            .setView(scrollView)
            .setPositiveButton("I Accept") { _, _ -> binding.cbPrivacyPolicy.isChecked = true }
            .setNegativeButton("Close", null)
            .show()
    }

    companion object {
        private val PRIVACY_POLICY_TEXT = """
PRIVACY POLICY & TERMS OF SERVICE

1. DATA COLLECTION & USE
We collect personal information (name, email, phone number, national ID, and financial data) solely for the purpose of providing SACCO services including account management, loan processing, and transaction notifications.

2. DATA SECURITY
Your data is protected using industry-standard encryption in transit (TLS/SSL) and at rest. Access to your account is secured via PIN and OTP verification.

3. ELECTRONIC COMMUNICATIONS
By registering, you consent to receive SMS and email notifications related to your account activity, including OTP codes, transaction alerts, and service announcements.

4. ACCOUNT RESPONSIBILITIES
You agree to provide accurate information and to keep your PIN confidential. The SACCO reserves the right to suspend accounts suspected of fraudulent activity.

5. FINANCIAL TRANSACTIONS
All transactions initiated through this app are subject to the SACCO's existing by-laws, lending policies, and fee schedules.

6. DATA RETENTION
We retain your data for the duration of your membership and as required by financial regulations.

7. YOUR RIGHTS
You have the right to access, correct, or request deletion of your personal data, subject to regulatory requirements.

8. CHANGES TO THIS POLICY
We may update this policy from time to time. Continued use of the app constitutes acceptance of any changes.

9. CONTACT
For privacy-related queries, contact your SACCO branch office.
        """.trimIndent()
    }
}
