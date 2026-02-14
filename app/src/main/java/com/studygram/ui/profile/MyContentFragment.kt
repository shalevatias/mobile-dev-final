package com.studygram.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository
import com.studygram.databinding.FragmentMyContentBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.ui.feed.PostAdapter
import com.studygram.utils.PreferenceManager
import com.studygram.utils.gone
import com.studygram.utils.visible

class MyContentFragment : BaseFragment<FragmentMyContentBinding>() {

    private val viewModel: MyContentViewModel by viewModels {
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
        MyContentViewModelFactory(postRepository, authRepository)
    }

    private lateinit var postAdapter: PostAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMyContentBinding.inflate(inflater, container, false)

    override fun setupUI() {
        postAdapter = PostAdapter(
            onPostClick = { post ->
                val action = MyContentFragmentDirections
                    .actionMyContentFragmentToPostDetailFragment(post.id)
                findNavController().navigate(action)
            },
            onLikeClick = { post ->
                viewModel.likePost(post.id)
            },
            currentUserId = viewModel.currentUserId
        )

        binding.recyclerViewPosts.adapter = postAdapter

        binding.fabCreatePost.setOnClickListener {
            findNavController().navigate(R.id.action_myContentFragment_to_createPostFragment)
        }
    }

    override fun observeData() {
        viewModel.myPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            if (posts.isEmpty()) {
                binding.tvEmptyState.visible()
                binding.recyclerViewPosts.gone()
            } else {
                binding.tvEmptyState.gone()
                binding.recyclerViewPosts.visible()
            }
        }
    }
}
