package com.nodimobilebanking.ui.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nodimobilebanking.data.api.RetrofitClient
import com.nodimobilebanking.data.repository.SaccoRepository
import com.nodimobilebanking.databinding.ActivitySelfRegisterBinding
import com.nodimobilebanking.utils.TokenManager
import com.nodimobilebanking.utils.toast

class SelfRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelfRegisterBinding
    lateinit var viewModel: SelfRegisterViewModel

    private val stepLabels = arrayOf(
        "Personal Details", "Contact & Identity", "Documents", "Next of Kin", "Review & Pay"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelfRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tokenManager = TokenManager(applicationContext)
        val repo = SaccoRepository(RetrofitClient.getPublicInstance(applicationContext), tokenManager)
        viewModel = SelfRegisterViewModel(repo)

        setupViewPager()
        setupButtons()
        updateStepUI(0)

        viewModel.loadCategories()

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 5
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> SelfRegStep1Fragment()
                1 -> SelfRegStep2Fragment()
                2 -> SelfRegStep3Fragment()
                3 -> SelfRegStep4Fragment()
                4 -> SelfRegStep5Fragment()
                else -> throw IllegalStateException()
            }
        }
        binding.viewPager.isUserInputEnabled = false
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current > 0) {
                binding.viewPager.currentItem = current - 1
                updateStepUI(current - 1)
            }
        }

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            val fragment = supportFragmentManager.findFragmentByTag("f$current")
                ?: supportFragmentManager.fragments.getOrNull(current)

            if (fragment is StepValidator && !fragment.validate()) return@setOnClickListener

            if (fragment is StepDataSaver) fragment.saveData()

            if (current < 4) {
                binding.viewPager.currentItem = current + 1
                updateStepUI(current + 1)
            } else {
                onFinalSubmit()
            }
        }
    }

    private fun onFinalSubmit() {
        val step5 = supportFragmentManager.fragments.filterIsInstance<SelfRegStep5Fragment>().firstOrNull()
        step5?.startSubmission()
    }

    fun updateStepUI(position: Int) {
        binding.tvStepLabel.text = stepLabels[position]
        binding.tvStepCount.text = "Step ${position + 1} of 5"
        binding.progressBar.progress = ((position + 1) * 100) / 5

        binding.btnBack.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE

        if (position == 4) {
            binding.btnNext.text = "Submit & Pay"
        } else {
            binding.btnNext.text = "Next"
        }
    }

    fun setBottomButtonsEnabled(enabled: Boolean) {
        binding.btnBack.isEnabled = enabled
        binding.btnNext.isEnabled = enabled
    }

    interface StepValidator {
        fun validate(): Boolean
    }

    interface StepDataSaver {
        fun saveData()
    }
}
