package com.studygram.ui.feed

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.fragment.app.viewModels
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

    private val viewModel: FeedViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(requireContext())
        val authManager = FirebaseAuthManager()
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
        FeedViewModelFactory(postRepository, authRepository)
    }

    private lateinit var postAdapter: PostAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFeedBinding.inflate(inflater, container, false)

    override fun setupUI() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                val action = FeedFragmentDirections
                    .actionFeedFragmentToPostDetailFragment(post.id)
                findNavController().navigate(action)
            },
            onLikeClick = { post ->
                viewModel.likePost(post.id)
            },
            currentUserId = viewModel.currentUserId
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

        binding.btnRefreshQuote.setOnClickListener {
            viewModel.loadQuote()
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
                }
                is Resource.Error -> {
                    binding.swipeRefresh.isRefreshing = false
                    showToast(resource.message ?: "Sync failed")
                }
            }
        }

        viewModel.quote.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressQuote.visible()
                    binding.tvQuoteContent.gone()
                    binding.tvQuoteAuthor.gone()
                    binding.tvQuoteError.gone()
                }
                is Resource.Success -> {
                    binding.progressQuote.gone()
                    binding.tvQuoteError.gone()
                    resource.data?.let { quote ->
                        binding.tvQuoteContent.visible()
                        binding.tvQuoteAuthor.visible()
                        binding.tvQuoteContent.text = "\"${quote.content}\""
                        binding.tvQuoteAuthor.text = "— ${quote.author}"
                    }
                }
                is Resource.Error -> {
                    binding.progressQuote.gone()
                    binding.tvQuoteContent.gone()
                    binding.tvQuoteAuthor.gone()
                    binding.tvQuoteError.visible()
                    binding.tvQuoteError.text = resource.message ?: "Failed to load quote"
                }
            }
        }
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
            R.id.action_profile -> {
                findNavController().navigate(R.id.action_feedFragment_to_profileFragment)
                true
            }
            R.id.action_my_content -> {
                findNavController().navigate(R.id.action_feedFragment_to_myContentFragment)
                true
            }
            R.id.action_logout -> {
                viewModel.logout()
                findNavController().navigate(R.id.action_feedFragment_to_loginFragment)
                true
            }
            else -> false
        }
    }
}
