package com.studygram.ui.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.AuthRepository
import com.studygram.databinding.FragmentRegisterBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import com.studygram.utils.gone
import com.studygram.utils.visible

class RegisterFragment : BaseFragment<FragmentRegisterBinding>() {

    private val viewModel: AuthViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val authManager = FirebaseAuthManager()
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(requireContext())
        val authRepository = AuthRepository(
            authManager,
            firestoreManager,
            database.userDao(),
            preferenceManager
        )
        AuthViewModelFactory(authRepository)
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentRegisterBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            viewModel.signUp(email, password, username)
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun observeData() {
        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.btnRegister.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    binding.btnRegister.isEnabled = true
                    showToast("Registration successful")
                    findNavController().navigate(R.id.action_registerFragment_to_feedFragment)
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.btnRegister.isEnabled = true
                    showToast(resource.message ?: "Registration failed")
                }
            }
        }
    }
}
