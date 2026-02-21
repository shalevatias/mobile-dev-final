package com.studygram.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.AuthRepository
import com.studygram.databinding.FragmentProfileBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import com.studygram.utils.gone
import com.studygram.utils.hideKeyboard
import com.studygram.utils.visible

class ProfileFragment : BaseFragment<FragmentProfileBinding>() {

    private val viewModel: ProfileViewModel by lazy {
        val context = requireContext()
        val database = AppDatabase.getDatabase(context)
        val authManager = FirebaseAuthManager()
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(context)
        val authRepository = AuthRepository(
            context,
            authManager,
            firestoreManager,
            database.userDao(),
            preferenceManager
        )
        val factory = ProfileViewModelFactory(authRepository)
        ViewModelProvider(this, factory)[ProfileViewModel::class.java]
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentProfileBinding.inflate(inflater, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        try {
            android.util.Log.d("ProfileFragment", "onViewCreated called")
            super.onViewCreated(view, savedInstanceState)
            android.util.Log.d("ProfileFragment", "onViewCreated completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error in onViewCreated", e)
            showError("Failed to load profile: ${e.message}")
        }
    }

    override fun setupUI() {
        binding.btnSave.setOnClickListener {
            hideKeyboard()
            saveProfile()
        }

        binding.btnLogout.setOnClickListener {
            showConfirmationDialog(
                title = getString(R.string.logout),
                message = getString(R.string.confirm_logout),
                positiveButtonText = getString(R.string.logout),
                onConfirm = {
                    viewModel.logout()
                    findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
                }
            )
        }
    }

    override fun observeData() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvEmail.text = it.email
                binding.etName.setText(it.username)
                binding.etYearOfStudy.setText(it.yearOfStudy)
                binding.etDegree.setText(it.degree)
            }
        }

        viewModel.updateState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.btnSave.isEnabled = false
                    binding.tilName.isEnabled = false
                    binding.tilYearOfStudy.isEnabled = false
                    binding.tilDegree.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    binding.btnSave.isEnabled = true
                    binding.tilName.isEnabled = true
                    binding.tilYearOfStudy.isEnabled = true
                    binding.tilDegree.isEnabled = true
                    showSuccess(getString(R.string.success_profile_updated))
                    viewModel.clearUpdateState()
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.btnSave.isEnabled = true
                    binding.tilName.isEnabled = true
                    binding.tilYearOfStudy.isEnabled = true
                    binding.tilDegree.isEnabled = true
                    showError(resource.message ?: getString(R.string.error_update_failed))
                    viewModel.clearUpdateState()
                }
                null -> {
                    binding.progressBar.gone()
                    binding.btnSave.isEnabled = true
                    binding.tilName.isEnabled = true
                    binding.tilYearOfStudy.isEnabled = true
                    binding.tilDegree.isEnabled = true
                }
            }
        }
    }

    private fun saveProfile() {
        // Clear previous errors
        binding.tilName.error = null
        binding.tilYearOfStudy.error = null
        binding.tilDegree.error = null

        val name = binding.etName.text.toString().trim()
        val yearOfStudy = binding.etYearOfStudy.text.toString().trim()
        val degree = binding.etDegree.text.toString().trim()

        // Validation
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.validation_name_required)
            isValid = false
        }

        if (yearOfStudy.isEmpty()) {
            binding.tilYearOfStudy.error = getString(R.string.validation_year_required)
            isValid = false
        }

        if (degree.isEmpty()) {
            binding.tilDegree.error = getString(R.string.validation_degree_required)
            isValid = false
        }

        if (!isValid) {
            showError(getString(R.string.error_empty_field))
            return
        }

        // Update profile
        viewModel.updateProfile(name, yearOfStudy, degree)
    }
}
