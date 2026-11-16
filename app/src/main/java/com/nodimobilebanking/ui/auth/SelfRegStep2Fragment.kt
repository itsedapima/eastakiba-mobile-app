package com.nodimobilebanking.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nodimobilebanking.databinding.FragmentSelfregStep2Binding
import com.nodimobilebanking.utils.toast

class SelfRegStep2Fragment : Fragment(),
    SelfRegisterActivity.StepValidator,
    SelfRegisterActivity.StepDataSaver {

    private var _binding: FragmentSelfregStep2Binding? = null
    private val binding get() = _binding!!
    private val vm get() = (requireActivity() as SelfRegisterActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSelfregStep2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreData()
    }

    override fun validate(): Boolean {
        val phone = binding.etPhone.text.toString().trim()
        if (phone.isBlank()) { requireContext().toast("Phone number is required"); return false }
        if (!phone.matches(Regex("^(254|0)[17]\\d{8}$"))) {
            requireContext().toast("Enter a valid Kenyan phone number (07xx or 01xx or 254xx)"); return false
        }
        if (binding.etEmail.text.isNullOrBlank()) {
            requireContext().toast("Email address is required"); return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(binding.etEmail.text.toString().trim()).matches()) {
            requireContext().toast("Enter a valid email address"); return false
        }
        if (binding.etNationalId.text.isNullOrBlank()) {
            requireContext().toast("National ID is required"); return false
        }
        return true
    }

    override fun saveData() {
        vm.phone = binding.etPhone.text.toString().trim()
        vm.email = binding.etEmail.text.toString().trim()
        vm.nationalId = binding.etNationalId.text.toString().trim()
        vm.kraPin = binding.etKraPin.text.toString().trim()
        vm.town = binding.etTown.text.toString().trim()
        vm.postalAddress = binding.etPostalAddress.text.toString().trim()
    }

    private fun restoreData() {
        if (vm.phone.isNotBlank()) binding.etPhone.setText(vm.phone)
        if (vm.email.isNotBlank()) binding.etEmail.setText(vm.email)
        if (vm.nationalId.isNotBlank()) binding.etNationalId.setText(vm.nationalId)
        if (vm.kraPin.isNotBlank()) binding.etKraPin.setText(vm.kraPin)
        if (vm.town.isNotBlank()) binding.etTown.setText(vm.town)
        if (vm.postalAddress.isNotBlank()) binding.etPostalAddress.setText(vm.postalAddress)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
