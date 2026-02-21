package com.studygram.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.studygram.R
import com.studygram.databinding.FragmentRegisterBinding
import com.studygram.utils.FormValidator
import com.studygram.utils.hideKeyboard
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(requireContext())
    }

    private val formValidator = FormValidator()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupRealTimeValidation()
        observeAuthState()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            // Clear previous errors
            formValidator.clearErrors(binding.tilUsername, binding.tilEmail, binding.tilPassword)
            hideKeyboard()

            if (validateInput()) {
                val username = binding.etUsername.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString().trim()
                viewModel.signUp(email, password, username)
            }
        }

        binding.tvSignInLink.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRealTimeValidation() {
        binding.etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etUsername.text.toString().isNotEmpty()) {
                formValidator.validateUsername(binding.tilUsername)
            }
        }

        binding.etEmail.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etEmail.text.toString().isNotEmpty()) {
                formValidator.validateEmail(binding.tilEmail)
            }
        }

        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etPassword.text.toString().isNotEmpty()) {
                formValidator.validatePassword(binding.tilPassword)
            }
        }
    }

    /**
     * Validate all input fields using FormValidator
     * @return True if all fields are valid, false otherwise
     */
    private fun validateInput(): Boolean {
        val isUsernameValid = formValidator.validateUsername(binding.tilUsername)
        val isEmailValid = formValidator.validateEmail(binding.tilEmail)
        val isPasswordValid = formValidator.validatePassword(binding.tilPassword)

        return isUsernameValid && isEmailValid && isPasswordValid
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when {
                    state.isLoading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnRegister.isEnabled = false
                        binding.tvError.visibility = View.GONE
                        binding.tilUsername.isEnabled = false
                        binding.tilEmail.isEnabled = false
                        binding.tilPassword.isEnabled = false
                    }
                    state.error != null -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = state.error
                        binding.tilUsername.isEnabled = true
                        binding.tilEmail.isEnabled = true
                        binding.tilPassword.isEnabled = true

                        viewModel.clearError()
                    }
                    state.user != null -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Welcome to StudyGram!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registerFragment_to_feedFragment)
                    }
                    else -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegister.isEnabled = true
                        binding.tilUsername.isEnabled = true
                        binding.tilEmail.isEnabled = true
                        binding.tilPassword.isEnabled = true
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
