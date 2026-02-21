package com.studygram.ui.comments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studygram.R
import com.studygram.data.model.Comment
import com.studygram.databinding.ItemCommentBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val currentUserId: String?,
    private val onDeleteClick: (Comment) -> Unit
) : ListAdapter<Comment, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            binding.apply {
                tvCommentAuthorName.text = comment.authorName
                tvCommentContent.text = comment.content
                tvCommentTimestamp.text = formatTimestamp(comment.timestamp)

                // Author image
                if (comment.authorImageUrl != null) {
                    Picasso.get()
                        .load(comment.authorImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(ivCommentAuthorImage)
                } else {
                    ivCommentAuthorImage.setImageResource(R.drawable.ic_profile_placeholder)
                }

                // Show delete button only if comment belongs to current user
                if (comment.userId == currentUserId) {
                    btnDeleteComment.visibility = View.VISIBLE
                    btnDeleteComment.setOnClickListener { onDeleteClick(comment) }
                } else {
                    btnDeleteComment.visibility = View.GONE
                }
            }
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
                    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}
