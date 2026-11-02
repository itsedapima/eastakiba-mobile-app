package com.nodimobilebanking.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nodimobilebanking.databinding.ActivitySplashBinding
import com.nodimobilebanking.ui.dashboard.MainActivity
import com.nodimobilebanking.utils.CompanyInfoManager
import com.nodimobilebanking.utils.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(applicationContext)

        // Pre-fetch company branding so it's ready for login/dashboard
        CompanyInfoManager.refreshAsync(this)

        lifecycleScope.launch {
            // Show cached company name while splash is visible
            binding.tvSplashName.text = tokenManager.getCompanyName()
            delay(1800)
            val isLoggedIn = tokenManager.isLoggedIn()
            val intent = if (isLoggedIn) {
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                Intent(this@SplashActivity, LoginActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }
}
