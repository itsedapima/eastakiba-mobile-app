package com.nodimobilebanking.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.nodimobilebanking.R
import com.nodimobilebanking.databinding.FragmentSelfregStep1Binding
import com.nodimobilebanking.utils.Resource
import com.nodimobilebanking.utils.toast
import java.text.SimpleDateFormat
import java.util.*

class SelfRegStep1Fragment : Fragment(),
    SelfRegisterActivity.StepValidator,
    SelfRegisterActivity.StepDataSaver {

    private var _binding: FragmentSelfregStep1Binding? = null
    private val binding get() = _binding!!
    private val vm get() = (requireActivity() as SelfRegisterActivity).viewModel

    private val categoryMap = mutableMapOf<String, String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSelfregStep1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val maritalItems = arrayOf("Married", "Single", "Separated", "Un-known")
        binding.actvMaritalStatus.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, maritalItems)
        )

        binding.etDob.setOnClickListener { showDatePicker() }

        vm.categories.observe(viewLifecycleOwner) { res ->
            when (res) {
                is Resource.Success -> {
                    val cats = res.data!!
                    categoryMap.clear()
                    cats.forEach { categoryMap[it.displayName] = it.category }
                    val names = cats.map { it.displayName }.toTypedArray()
                    binding.actvMemberType.setAdapter(
                        ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
                    )
                }
                is Resource.Error -> requireContext().toast(res.message ?: "Failed to load categories")
                is Resource.Loading -> {}
            }
        }

        binding.actvMemberType.setOnItemClickListener { _, _, pos, _ ->
            val displayName = binding.actvMemberType.text.toString()
            val cat = categoryMap[displayName] ?: ""
            val fee = vm.getFeeForCategory(cat)
            if (fee > 0) {
                binding.tvFeeInfo.text = "Registration fee: KES ${String.format("%,.0f", fee)}"
                binding.tvFeeInfo.visibility = View.VISIBLE
            } else {
                binding.tvFeeInfo.visibility = View.GONE
            }
        }

        restoreData()
    }

    private fun showDatePicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Date of Birth")
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            binding.etDob.setText(sdf.format(Date(millis)))
        }
        picker.show(parentFragmentManager, "dob")
    }

    override fun validate(): Boolean {
        if (binding.actvMemberType.text.isNullOrBlank()) {
            requireContext().toast("Please select a member category"); return false
        }
        if (binding.etFirstName.text.isNullOrBlank()) {
            requireContext().toast("First name is required"); return false
        }
        if (binding.etLastName.text.isNullOrBlank()) {
            requireContext().toast("Last name is required"); return false
        }
        if (binding.rgGender.checkedRadioButtonId == -1) {
            requireContext().toast("Please select your gender"); return false
        }
        if (binding.etDob.text.isNullOrBlank()) {
            requireContext().toast("Date of birth is required"); return false
        }
        return true
    }

    override fun saveData() {
        vm.firstName = binding.etFirstName.text.toString()
        vm.middleName = binding.etMiddleName.text.toString()
        vm.lastName = binding.etLastName.text.toString()
        vm.gender = if (binding.rbMale.isChecked) "Male" else "Female"
        val dobDisplay = binding.etDob.text.toString()
        vm.dob = try {
            val display = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val api = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            api.format(display.parse(dobDisplay)!!)
        } catch (_: Exception) { dobDisplay }
        val displayName = binding.actvMemberType.text.toString()
        vm.memberType = categoryMap[displayName] ?: displayName
        vm.maritalStatus = binding.actvMaritalStatus.text.toString().ifBlank { "Un-known" }
    }

    private fun restoreData() {
        if (vm.firstName.isNotBlank()) binding.etFirstName.setText(vm.firstName)
        if (vm.middleName.isNotBlank()) binding.etMiddleName.setText(vm.middleName)
        if (vm.lastName.isNotBlank()) binding.etLastName.setText(vm.lastName)
        if (vm.gender == "Male") binding.rbMale.isChecked = true
        else if (vm.gender == "Female") binding.rbFemale.isChecked = true
        if (vm.dob.isNotBlank()) {
            val display = try {
                val api = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val df = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                df.format(api.parse(vm.dob)!!)
            } catch (_: Exception) { vm.dob }
            binding.etDob.setText(display)
        }
        if (vm.maritalStatus.isNotBlank()) binding.actvMaritalStatus.setText(vm.maritalStatus, false)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
