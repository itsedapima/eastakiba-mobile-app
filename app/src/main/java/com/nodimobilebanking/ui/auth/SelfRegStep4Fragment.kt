package com.nodimobilebanking.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.nodimobilebanking.databinding.FragmentSelfregStep4Binding
import com.nodimobilebanking.utils.toast

class SelfRegStep4Fragment : Fragment(),
    SelfRegisterActivity.StepValidator,
    SelfRegisterActivity.StepDataSaver {

    private var _binding: FragmentSelfregStep4Binding? = null
    private val binding get() = _binding!!
    private val vm get() = (requireActivity() as SelfRegisterActivity).viewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSelfregStep4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val relationships = arrayOf("Son", "Daughter", "Parent", "Spouse", "Other")
        binding.actvKinRelationship.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, relationships)
        )

        restoreData()
    }

    override fun validate(): Boolean {
        if (binding.etKinName.text.isNullOrBlank()) {
            requireContext().toast("Next of kin name is required"); return false
        }
        if (binding.actvKinRelationship.text.isNullOrBlank()) {
            requireContext().toast("Relationship is required"); return false
        }
        return true
    }

    override fun saveData() {
        vm.kinName = binding.etKinName.text.toString().trim()
        vm.kinRelationship = binding.actvKinRelationship.text.toString().ifBlank { "Other" }
        vm.kinPhone = binding.etKinPhone.text.toString().trim()
        vm.kinNationalId = binding.etKinNationalId.text.toString().trim()
    }

    private fun restoreData() {
        if (vm.kinName.isNotBlank()) binding.etKinName.setText(vm.kinName)
        if (vm.kinRelationship.isNotBlank()) binding.actvKinRelationship.setText(vm.kinRelationship, false)
        if (vm.kinPhone.isNotBlank()) binding.etKinPhone.setText(vm.kinPhone)
        if (vm.kinNationalId.isNotBlank()) binding.etKinNationalId.setText(vm.kinNationalId)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
