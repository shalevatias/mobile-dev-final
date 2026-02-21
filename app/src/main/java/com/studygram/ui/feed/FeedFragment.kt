package com.studygram.ui.feed

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository
import com.studygram.databinding.FragmentFeedBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import com.studygram.utils.gone
import com.studygram.utils.visible

class FeedFragment : BaseFragment<FragmentFeedBinding>() {

    private val viewModel: FeedViewModel by lazy {
        val context = requireContext()
        val database = AppDatabase.getDatabase(context)
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(context)
        val authManager = FirebaseAuthManager()
        val postRepository = PostRepository(
            context,
            database.postDao(),
            firestoreManager,
            preferenceManager
        )
        val authRepository = AuthRepository(
            context,
            authManager,
            firestoreManager,
            database.userDao(),
            preferenceManager
        )
        val factory = FeedViewModelFactory(postRepository, authRepository)
        ViewModelProvider(this, factory)[FeedViewModel::class.java]
    }

    private lateinit var postAdapter: PostAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFeedBinding.inflate(inflater, container, false)

    override fun setupUI() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                // Post detail feature not implemented in current version
                showToast("Post: ${post.title}")
            },
            onLikeClick = { post ->
                viewModel.likePost(post.id)
            },
            onCommentClick = { post ->
                // Comments feature not implemented in current version
                showToast("Comments: ${post.commentsCount}")
            }
        )

        binding.recyclerViewPosts.adapter = postAdapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_createPostFragment)
        }

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            handleMenuClick(menuItem)
        }
    }

    override fun observeData() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            if (posts.isEmpty()) {
                binding.tvEmptyState.visible()
                binding.recyclerViewPosts.gone()
            } else {
                binding.tvEmptyState.gone()
                binding.recyclerViewPosts.visible()
            }
        }

        viewModel.courseTags.observe(viewLifecycleOwner) { courses ->
            setupCourseFilter(courses)
        }

        viewModel.syncState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.swipeRefresh.isRefreshing = true
                }
                is Resource.Success -> {
                    binding.swipeRefresh.isRefreshing = false
                    // Optional: Show success message
                    // showSuccess("Posts refreshed")
                }
                is Resource.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    // Silently fail - user can still see cached posts
                    // No error message shown to avoid annoying the user
                }
            }
        }

        // Observe like state
        viewModel.likeState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    // Optional: Show loading indicator for like action
                }
                is Resource.Success -> {
                    // Like succeeded - UI already updated via LiveData
                }
                is Resource.Error -> {
                    showError(resource.message ?: "Failed to like post")
                }
            }
        }

        // Observe general errors
        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                // Only show non-network errors
                if (!it.contains("internet", ignoreCase = true) &&
                    !it.contains("network", ignoreCase = true) &&
                    !it.contains("connection", ignoreCase = true)) {
                    showError(it)
                }
                viewModel.clearError()
            }
        }
    }

    override fun onRetryAfterError(exception: Throwable) {
        // Retry the last operation based on context
        viewModel.syncPosts()
    }

    private fun setupCourseFilter(courses: List<String>) {
        binding.chipGroupCourses.removeAllViews()

        val allChip = Chip(requireContext()).apply {
            text = getString(R.string.all_courses)
            isCheckable = true
            isChecked = true
            setOnClickListener {
                viewModel.filterByCourse(null)
            }
        }
        binding.chipGroupCourses.addView(allChip)

        courses.forEach { course ->
            val chip = Chip(requireContext()).apply {
                text = course
                isCheckable = true
                setOnClickListener {
                    viewModel.filterByCourse(course)
                }
            }
            binding.chipGroupCourses.addView(chip)
        }
    }

    private fun handleMenuClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_logout -> {
                showConfirmationDialog(
                    title = getString(R.string.logout),
                    message = getString(R.string.confirm_logout),
                    positiveButtonText = getString(R.string.logout),
                    onConfirm = {
                        viewModel.logout()
                        findNavController().navigate(R.id.loginFragment)
                    }
                )
                true
            }
            else -> false
        }
    }
}
