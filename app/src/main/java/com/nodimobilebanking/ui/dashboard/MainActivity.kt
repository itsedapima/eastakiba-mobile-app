package com.nodimobilebanking.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.nodimobilebanking.R
import com.nodimobilebanking.databinding.ActivityMainBinding
import com.nodimobilebanking.ui.accounts.AccountsFragment
import com.nodimobilebanking.ui.auth.LoginActivity
import com.nodimobilebanking.ui.profile.ProfileFragment
import android.os.Handler
import android.os.Looper
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.api.TokenAuthenticator
import com.nodimobilebanking.utils.TokenManager
import com.nodimobilebanking.utils.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager

    companion object {
        private const val INACTIVITY_TIMEOUT_MS = 5L * 60 * 1000
    }

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (!isFinishing) {
            logout()
        }
    }

    // ── Back-button state ────────────────────────────────────────────
    private var lastBackPressTime = 0L
    private val doubleBackToExitMs = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(applicationContext)

        // Update company-name header if branding is cached
        refreshCompanyHeader()

        // Listen for forced logout (token expired)
        val logoutReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                logout()
            }
        }
        registerReceiver(
            logoutReceiver,
            IntentFilter(TokenAuthenticator.ACTION_FORCE_LOGOUT),
            Context.RECEIVER_NOT_EXPORTED
        )

        resetInactivityTimer()

        if (savedInstanceState == null) loadFragment(DashboardFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_accounts  -> loadFragment(AccountsFragment())
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_profile   -> loadFragment(ProfileFragment())
            }
            true
        }
        binding.bottomNav.selectedItemId = R.id.nav_dashboard

        // ── Back button: if not on dashboard, navigate to dashboard.
        //    If on dashboard, require double-tap to exit ──────────────
        onBackPressedDispatcher.addCallback(this, object :
            androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentId = binding.bottomNav.selectedItemId
                if (currentId != R.id.nav_dashboard) {
                    // Not on home → go home
                    binding.bottomNav.selectedItemId = R.id.nav_dashboard
                } else {
                    // On home → require double-tap to exit
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < doubleBackToExitMs) {
                        finish()
                    } else {
                        lastBackPressTime = now
                        toast("Press back again to exit")
                    }
                }
            }
        })
    }

    private fun refreshCompanyHeader() {
        val header = binding.root.findViewById<android.widget.TextView>(R.id.tvCompanyHeader)
        header ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val name = try { tokenManager.getCompanyName() } catch (_: Exception) { "SACCO MIS" }
            runOnUiThread { header.text = name.uppercase() }
        }
    }

    fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
            TokenManager(applicationContext).clearTokens()
            runOnUiThread {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finishAffinity()
            }
        }
    }

    // Public method for fragments to load other fragments (e.g., DashboardFragment -> TransactionsFragment)
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        resetInactivityTimer()
        return super.dispatchTouchEvent(ev)
    }

    private fun resetInactivityTimer() {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, INACTIVITY_TIMEOUT_MS)
    }

    fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commitAllowingStateLoss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
