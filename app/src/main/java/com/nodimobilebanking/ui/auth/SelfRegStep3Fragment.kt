package com.nodimobilebanking.ui.auth

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nodimobilebanking.databinding.FragmentSelfregStep3Binding
import com.nodimobilebanking.utils.toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SelfRegStep3Fragment : Fragment(), SelfRegisterActivity.StepValidator {

    private var _binding: FragmentSelfregStep3Binding? = null
    private val binding get() = _binding!!
    private val vm get() = (requireActivity() as SelfRegisterActivity).viewModel

    private var currentDocType = ""
    private var cameraUri: Uri? = null

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else requireContext().toast("Camera permission is required to take photos")
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && cameraUri != null) {
            setDocUri(currentDocType, cameraUri!!)
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) setDocUri(currentDocType, uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSelfregStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardPassportPhoto.setOnClickListener { showImagePicker("passport_photo") }
        binding.cardIdFront.setOnClickListener { showImagePicker("id_front") }
        binding.cardIdBack.setOnClickListener { showImagePicker("id_back") }

        restoreImages()
    }

    private fun getDocUri(docType: String): Uri? = when (docType) {
        "passport_photo" -> vm.passportPhotoUri
        "id_front" -> vm.idFrontUri
        "id_back" -> vm.idBackUri
        else -> null
    }

    private fun showImagePicker(docType: String) {
        currentDocType = docType
        val title = when(docType) {
            "passport_photo" -> "Passport Photo"
            "id_front" -> "ID Front"
            else -> "ID Back"
        }
        val hasImage = getDocUri(docType) != null
        val options = mutableListOf("Take Photo", "Choose from Gallery")
        if (hasImage) options.add("View Photo")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            cameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                    1 -> galleryLauncher.launch("image/*")
                    2 -> showFullImage(docType, title)
                }
            }
            .show()
    }

    private fun showFullImage(docType: String, title: String) {
        val uri = getDocUri(docType) ?: return
        val imageView = android.widget.ImageView(requireContext()).apply {
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        Glide.with(this).load(uri).into(imageView)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(imageView)
            .setPositiveButton("OK", null)
            .setNegativeButton("Retake") { _, _ -> showImagePicker(docType) }
            .show()
    }

    private fun launchCamera() {
        val file = File.createTempFile(
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}_",
            ".jpg",
            requireContext().cacheDir
        )
        cameraUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
        cameraLauncher.launch(cameraUri!!)
    }

    private fun setDocUri(docType: String, uri: Uri) {
        when (docType) {
            "passport_photo" -> {
                vm.passportPhotoUri = uri
                loadImage(uri, binding.ivPassportPhoto)
                binding.tvPassportLabel.text = "Passport Photo (uploaded)"
            }
            "id_front" -> {
                vm.idFrontUri = uri
                loadImage(uri, binding.ivIdFront)
                binding.tvIdFrontLabel.text = "ID Front (uploaded)"
            }
            "id_back" -> {
                vm.idBackUri = uri
                loadImage(uri, binding.ivIdBack)
                binding.tvIdBackLabel.text = "ID Back (uploaded)"
            }
        }
    }

    private fun loadImage(uri: Uri, imageView: android.widget.ImageView) {
        Glide.with(this).load(uri).centerCrop().into(imageView)
        imageView.imageTintList = null
    }

    private fun restoreImages() {
        vm.passportPhotoUri?.let { setDocUri("passport_photo", it) }
        vm.idFrontUri?.let { setDocUri("id_front", it) }
        vm.idBackUri?.let { setDocUri("id_back", it) }
    }

    override fun validate(): Boolean {
        if (vm.passportPhotoUri == null) { requireContext().toast("Passport photo is required"); return false }
        if (vm.idFrontUri == null) { requireContext().toast("ID front page is required"); return false }
        if (vm.idBackUri == null) { requireContext().toast("ID back page is required"); return false }
        return true
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
