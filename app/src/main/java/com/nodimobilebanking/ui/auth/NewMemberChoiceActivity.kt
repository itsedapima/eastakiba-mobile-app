package com.nodimobilebanking.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nodimobilebanking.databinding.ActivityNewMemberChoiceBinding
import com.nodimobilebanking.utils.TokenManager
import kotlinx.coroutines.launch

class NewMemberChoiceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewMemberChoiceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewMemberChoiceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tokenManager = TokenManager(applicationContext)
        binding.tvCompanyName.text = "Your SACCO"
        lifecycleScope.launch {
            val name = tokenManager.getCompanyName()
            if (name.isNotBlank()) binding.tvCompanyName.text = name
        }

        binding.cardSelfRegister.setOnClickListener {
            startActivity(Intent(this, SelfRegisterActivity::class.java))
        }

        binding.cardActivateAccount.setOnClickListener {
            startActivity(Intent(this, OnboardStep1Activity::class.java))
        }

        binding.tvBackToLogin.setOnClickListener { finish() }
    }
}
