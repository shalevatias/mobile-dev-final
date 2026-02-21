package com.studygram.ui.comments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.studygram.R
import com.studygram.data.local.AppDatabase
import com.studygram.data.model.Comment
import com.studygram.data.remote.FirestoreManager
import com.studygram.databinding.DialogCommentsBinding
import com.studygram.utils.gone
import com.studygram.utils.visible

class CommentsDialogFragment : DialogFragment() {

    private var _binding: DialogCommentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommentsViewModel
    private lateinit var commentAdapter: CommentAdapter

    private val postId: String by lazy {
        arguments?.getString(ARG_POST_ID) ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Studygram_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupUI()
        observeData()

        if (postId.isNotEmpty()) {
            viewModel.loadComments(postId)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    private fun setupViewModel() {
        val context = requireContext()
        val database = AppDatabase.getDatabase(context)
        val firestoreManager = FirestoreManager()
        val factory = CommentsViewModelFactory(
            context,
            database.commentDao(),
            database.postDao(),
            firestoreManager
        )
        viewModel = ViewModelProvider(this, factory)[CommentsViewModel::class.java]
    }

    private fun setupUI() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        commentAdapter = CommentAdapter(
            currentUserId = currentUserId,
            onDeleteClick = { comment ->
                showDeleteConfirmation(comment)
            }
        )

        binding.recyclerViewComments.adapter = commentAdapter

        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.btnSendComment.setOnClickListener {
            sendComment()
        }

        binding.etComment.setOnEditorActionListener { _, _, _ ->
            sendComment()
            true
        }
    }

    private fun observeData() {
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            commentAdapter.submitList(comments)
            if (comments.isEmpty()) {
                binding.tvEmptyComments.visible()
                binding.recyclerViewComments.gone()
            } else {
                binding.tvEmptyComments.gone()
                binding.recyclerViewComments.visible()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        viewModel.success.observe(viewLifecycleOwner) { success ->
            success?.let {
                // Optional: Show success toast
                // Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearSuccess()
            }
        }
    }

    private fun sendComment() {
        val content = binding.etComment.text?.toString()?.trim() ?: ""

        if (content.isEmpty()) {
            binding.tilComment.error = getString(R.string.validation_comment_too_short)
            return
        }

        binding.tilComment.error = null
        viewModel.addComment(postId, content)
        binding.etComment.text?.clear()

        // Hide keyboard
        binding.etComment.clearFocus()
    }

    private fun showDeleteConfirmation(comment: Comment) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_comment)
            .setMessage(R.string.confirm_delete_comment)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteComment(postId, comment.id)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_POST_ID = "post_id"

        fun newInstance(postId: String): CommentsDialogFragment {
            return CommentsDialogFragment().apply {
                arguments = bundleOf(ARG_POST_ID to postId)
            }
        }
    }
}
