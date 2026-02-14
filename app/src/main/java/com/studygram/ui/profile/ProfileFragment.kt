package com.studygram.ui.profile

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.UserRepository
import com.studygram.databinding.FragmentProfileBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import com.studygram.utils.gone
import com.studygram.utils.loadImage
import com.studygram.utils.visible

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(requireContext())
        val authManager = FirebaseAuthManager()
        val storageManager = FirebaseStorageManager()
        val authRepository = AuthRepository(
            authManager,
            firestoreManager,
            database.userDao(),
            preferenceManager
        )
        val userRepository = UserRepository(
            database.userDao(),
            firestoreManager
        )
        ProfileViewModelFactory(authRepository, userRepository, storageManager)
    }

    private var uploadedImageUrl: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            binding.ivProfileImage.setImageURI(it)
            viewModel.uploadProfileImage(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            binding.ivProfileImage.setImageBitmap(it)
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.btnChangeImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    override fun observeData() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.etUsername.setText(it.username)
                binding.etEmail.setText(it.email)

                if (it.profileImageUrl != null) {
                    binding.ivProfileImage.loadImage(it.profileImageUrl, R.drawable.ic_profile_placeholder)
                    uploadedImageUrl = it.profileImageUrl
                } else {
                    binding.ivProfileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            }
        }

        viewModel.imageUploadState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.btnSave.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    binding.btnSave.isEnabled = true
                    uploadedImageUrl = resource.data
                    showToast("Image uploaded successfully")
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.btnSave.isEnabled = true
                    showToast(resource.message ?: "Image upload failed")
                }
            }
        }

        viewModel.updateState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.btnSave.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    binding.btnSave.isEnabled = true
                    showToast("Profile updated successfully")
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.btnSave.isEnabled = true
                    showToast(resource.message ?: "Failed to update profile")
                }
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Gallery", "Camera")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> cameraLauncher.launch(null)
                }
            }
            .show()
    }

    private fun saveProfile() {
        val username = binding.etUsername.text.toString().trim()
        viewModel.updateProfile(username, uploadedImageUrl)
    }

    private fun showLogoutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                viewModel.logout()
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
