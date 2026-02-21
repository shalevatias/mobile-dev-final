package com.studygram.ui.createpost

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.model.Post
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.PostRepository
import com.studygram.databinding.FragmentCreatePostBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.utils.FormValidator
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import com.studygram.utils.hideKeyboard
import kotlinx.coroutines.launch

class CreatePostFragment : BaseFragment<FragmentCreatePostBinding>() {

    private var selectedImageUri: Uri? = null
    private val formValidator = FormValidator()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivPreview.setImageURI(it)
            binding.ivPreview.visibility = View.VISIBLE
        }
    }

    private val viewModel: CreatePostViewModel by lazy {
        val context = requireContext()
        val database = AppDatabase.getDatabase(context)
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(context)
        val postRepository = PostRepository(
            context,
            database.postDao(),
            firestoreManager,
            preferenceManager
        )
        val factory = CreatePostViewModelFactory(postRepository, preferenceManager, context)
        ViewModelProvider(this, factory)[CreatePostViewModel::class.java]
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentCreatePostBinding.inflate(inflater, container, false)

    override fun setupUI() {
        binding.btnCreatePost.setOnClickListener {
            hideKeyboard()
            createPost()
        }

        binding.btnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        setupRealTimeValidation()
    }

    /**
     * Setup real-time validation to clear errors as user types
     */
    private fun setupRealTimeValidation() {
        binding.etTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etTitle.text.toString().isNotEmpty()) {
                formValidator.validatePostTitle(binding.tilTitle)
            }
        }

        binding.etContent.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etContent.text.toString().isNotEmpty()) {
                formValidator.validatePostContent(binding.tilContent)
            }
        }

        binding.etCourse.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && binding.etCourse.text.toString().isNotEmpty()) {
                formValidator.validateCourseTag(binding.tilCourse)
            }
        }
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.createPostState.collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnCreatePost.isEnabled = false
                        binding.btnAddImage.isEnabled = false
                        // Disable input fields during loading
                        binding.tilTitle.isEnabled = false
                        binding.tilContent.isEnabled = false
                        binding.tilCourse.isEnabled = false
                        binding.rgDifficulty.isEnabled = false
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        showSuccess(getString(R.string.success_post_created))
                        findNavController().navigateUp()
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnCreatePost.isEnabled = true
                        binding.btnAddImage.isEnabled = true
                        // Re-enable input fields
                        binding.tilTitle.isEnabled = true
                        binding.tilContent.isEnabled = true
                        binding.tilCourse.isEnabled = true
                        binding.rgDifficulty.isEnabled = true
                        showError(resource.message ?: "Failed to create post")
                    }
                    null -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnCreatePost.isEnabled = true
                        binding.btnAddImage.isEnabled = true
                        // Re-enable input fields
                        binding.tilTitle.isEnabled = true
                        binding.tilContent.isEnabled = true
                        binding.tilCourse.isEnabled = true
                        binding.rgDifficulty.isEnabled = true
                    }
                }
            }
        }
    }

    /**
     * Validate and create post
     */
    private fun createPost() {
        // Clear previous errors
        formValidator.clearErrors(binding.tilTitle, binding.tilContent, binding.tilCourse)

        // Validate all fields
        val isTitleValid = formValidator.validatePostTitle(binding.tilTitle)
        val isContentValid = formValidator.validatePostContent(binding.tilContent)
        val isCourseValid = formValidator.validateCourseTag(binding.tilCourse)

        if (!isTitleValid || !isContentValid || !isCourseValid) {
            // Show the first error to user
            formValidator.getFirstError()?.let { errorMsg ->
                showError(errorMsg)
            }
            return
        }

        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()

        // Get difficulty
        val difficulty = when (binding.rgDifficulty.checkedRadioButtonId) {
            R.id.rbEasy -> "Easy"
            R.id.rbMedium -> "Medium"
            R.id.rbHard -> "Hard"
            else -> "Easy"
        }

        // Create post
        val post = Post(
            id = "", // Will be generated
            title = title,
            content = content,
            courseTag = course,
            difficultyLevel = difficulty,
            userId = viewModel.currentUserId ?: "",
            authorName = viewModel.currentUserName ?: "Unknown"
        )

        viewModel.createPost(post, selectedImageUri)
    }
}
