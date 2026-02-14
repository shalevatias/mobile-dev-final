package com.studygram.ui.post

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.model.DifficultyLevel
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository
import com.studygram.databinding.FragmentCreatePostBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import com.studygram.utils.gone
import com.studygram.utils.visible
import kotlinx.coroutines.launch

class CreatePostFragment : BaseFragment<FragmentCreatePostBinding>() {

    private val args: CreatePostFragmentArgs by navArgs()

    private val viewModel: CreatePostViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(requireContext())
        val authManager = FirebaseAuthManager()
        val storageManager = FirebaseStorageManager()
        val postRepository = PostRepository(
            database.postDao(),
            firestoreManager,
            preferenceManager
        )
        val authRepository = AuthRepository(
            authManager,
            firestoreManager,
            database.userDao(),
            preferenceManager
        )
        CreatePostViewModelFactory(postRepository, storageManager, authRepository)
    }

    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private var isEditMode = false

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivPostImage.setImageURI(it)
            binding.ivPostImage.visible()
            binding.btnRemoveImage.visible()
            viewModel.uploadImage(it)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            binding.ivPostImage.setImageBitmap(it)
            binding.ivPostImage.visible()
            binding.btnRemoveImage.visible()
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentCreatePostBinding.inflate(inflater, container, false)

    override fun setupUI() {
        isEditMode = args.postId != null

        if (isEditMode) {
            binding.btnSubmit.text = getString(R.string.update_post)
            requireActivity().title = getString(R.string.update_post)
            loadPostForEdit()
        } else {
            binding.btnSubmit.text = getString(R.string.create_post)
            requireActivity().title = getString(R.string.create_post)
        }

        setupDifficultyDropdown()

        binding.btnAddImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            uploadedImageUrl = null
            binding.ivPostImage.setImageURI(null)
            binding.ivPostImage.gone()
            binding.btnRemoveImage.gone()
        }

        binding.btnSubmit.setOnClickListener {
            submitPost()
        }
    }

    override fun observeData() {
        viewModel.imageUploadState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.btnSubmit.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    binding.btnSubmit.isEnabled = true
                    uploadedImageUrl = resource.data
                    showToast("Image uploaded successfully")
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.btnSubmit.isEnabled = true
                    showToast(resource.message ?: "Image upload failed")
                }
            }
        }

        viewModel.createPostState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.btnSubmit.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    binding.btnSubmit.isEnabled = true
                    val message = if (isEditMode) "Post updated" else "Post created"
                    showToast(message)
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.btnSubmit.isEnabled = true
                    showToast(resource.message ?: "Failed to save post")
                }
            }
        }
    }

    private fun setupDifficultyDropdown() {
        val difficulties = DifficultyLevel.values().map { it.displayName }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            difficulties
        )
        binding.actvDifficulty.setAdapter(adapter)
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

    private fun loadPostForEdit() {
        val postId = args.postId ?: return
        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val post = database.postDao().getPostById(postId)
            post?.let {
                binding.etTitle.setText(it.title)
                binding.etContent.setText(it.content)
                binding.etCourse.setText(it.courseTag)
                binding.actvDifficulty.setText(it.difficultyLevel, false)

                if (it.imageUrl != null) {
                    uploadedImageUrl = it.imageUrl
                    binding.ivPostImage.setImageURI(Uri.parse(it.imageUrl))
                    binding.ivPostImage.visible()
                    binding.btnRemoveImage.visible()
                }
            }
        }
    }

    private fun submitPost() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()
        val course = binding.etCourse.text.toString().trim()
        val difficulty = binding.actvDifficulty.text.toString().trim()

        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val authManager = FirebaseAuthManager()
            val userId = authManager.currentUserId ?: return@launch
            val user = database.userDao().getUserById(userId)

            if (isEditMode) {
                val postId = args.postId!!
                val existingPost = database.postDao().getPostById(postId)
                if (existingPost != null) {
                    viewModel.updatePost(
                        postId = postId,
                        title = title,
                        content = content,
                        courseTag = course,
                        difficultyLevel = difficulty,
                        imageUrl = uploadedImageUrl,
                        existingPost = existingPost
                    )
                }
            } else {
                viewModel.createPost(
                    title = title,
                    content = content,
                    courseTag = course,
                    difficultyLevel = difficulty,
                    imageUrl = uploadedImageUrl,
                    authorName = user?.username ?: "Unknown",
                    authorImageUrl = user?.profileImageUrl
                )
            }
        }
    }
}
