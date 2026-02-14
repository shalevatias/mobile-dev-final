package com.studygram.ui.post

import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.CommentRepository
import com.studygram.data.repository.PostRepository
import com.studygram.databinding.FragmentPostDetailBinding
import com.studygram.ui.base.BaseFragment
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import com.studygram.utils.gone
import com.studygram.utils.loadImage
import com.studygram.utils.visible
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostDetailFragment : BaseFragment<FragmentPostDetailBinding>() {

    private val args: PostDetailFragmentArgs by navArgs()

    private val viewModel: PostDetailViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val firestoreManager = FirestoreManager()
        val preferenceManager = PreferenceManager(requireContext())
        val authManager = FirebaseAuthManager()
        val postRepository = PostRepository(
            database.postDao(),
            firestoreManager,
            preferenceManager
        )
        val commentRepository = CommentRepository(
            database.commentDao(),
            database.postDao(),
            firestoreManager
        )
        val authRepository = AuthRepository(
            authManager,
            firestoreManager,
            database.userDao(),
            preferenceManager
        )
        PostDetailViewModelFactory(postRepository, commentRepository, authRepository)
    }

    private lateinit var commentAdapter: CommentAdapter

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentPostDetailBinding.inflate(inflater, container, false)

    override fun setupUI() {
        viewModel.setPostId(args.postId)

        commentAdapter = CommentAdapter(
            currentUserId = viewModel.currentUserId,
            onDeleteClick = { comment ->
                showDeleteCommentDialog(comment.postId, comment.id)
            }
        )

        binding.recyclerViewComments.adapter = commentAdapter

        binding.btnLike.setOnClickListener {
            viewModel.likePost(args.postId)
        }

        binding.btnPostComment.setOnClickListener {
            postComment()
        }

        binding.fabEdit.setOnClickListener {
            val action = PostDetailFragmentDirections
                .actionPostDetailFragmentToCreatePostFragment(args.postId)
            findNavController().navigate(action)
        }

        setupMenu()
    }

    override fun observeData() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let { displayPost(it) }
        }

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentAdapter.submitList(comments)
            if (comments.isEmpty()) {
                binding.tvNoComments.visible()
                binding.recyclerViewComments.gone()
            } else {
                binding.tvNoComments.gone()
                binding.recyclerViewComments.visible()
            }
        }

        viewModel.commentState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.btnPostComment.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnPostComment.isEnabled = true
                    binding.etComment.text.clear()
                    showToast("Comment added")
                }
                is Resource.Error -> {
                    binding.btnPostComment.isEnabled = true
                    showToast(resource.message ?: "Failed to add comment")
                }
            }
        }

        viewModel.deleteState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                }
                is Resource.Success -> {
                    showToast("Post deleted")
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    showToast(resource.message ?: "Failed to delete post")
                }
            }
        }
    }

    private fun displayPost(post: com.studygram.data.model.Post) {
        binding.apply {
            tvAuthorName.text = post.authorName
            tvCourse.text = post.courseTag
            tvTimestamp.text = formatTimestamp(post.timestamp)
            tvTitle.text = post.title
            tvContent.text = post.content
            chipDifficulty.text = post.difficultyLevel
            tvLikes.text = post.likes.toString()

            if (post.authorImageUrl != null) {
                ivAuthorImage.loadImage(post.authorImageUrl, R.drawable.ic_profile_placeholder)
            } else {
                ivAuthorImage.setImageResource(R.drawable.ic_profile_placeholder)
            }

            if (post.imageUrl != null) {
                ivPostImage.loadImage(post.imageUrl)
                ivPostImage.visible()
            } else {
                ivPostImage.gone()
            }

            val isLiked = post.isLikedByUser(viewModel.currentUserId ?: "")
            btnLike.setIconResource(
                if (isLiked) R.drawable.ic_favorite_filled
                else R.drawable.ic_favorite_border
            )

            if (post.userId == viewModel.currentUserId) {
                fabEdit.visible()
            } else {
                fabEdit.gone()
            }
        }
    }

    private fun postComment() {
        val commentText = binding.etComment.text.toString().trim()
        if (commentText.isBlank()) {
            showToast("Comment cannot be empty")
            return
        }

        lifecycleScope.launch {
            val database = AppDatabase.getDatabase(requireContext())
            val authManager = FirebaseAuthManager()
            val userId = authManager.currentUserId ?: return@launch
            val user = database.userDao().getUserById(userId)

            viewModel.addComment(
                postId = args.postId,
                content = commentText,
                authorName = user?.username ?: "Unknown",
                authorImageUrl = user?.profileImageUrl
            )
        }
    }

    private fun showDeleteCommentDialog(postId: String, commentId: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete this comment?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteComment(postId, commentId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeletePostDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePost(args.postId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                viewModel.post.value?.let { post ->
                    if (post.userId == viewModel.currentUserId) {
                        menuInflater.inflate(R.menu.menu_post_detail, menu)
                    }
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete -> {
                        showDeletePostDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
