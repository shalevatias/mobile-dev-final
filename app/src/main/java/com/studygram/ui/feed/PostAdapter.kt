package com.studygram.ui.feed

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studygram.R
import com.studygram.data.model.Post
import com.studygram.databinding.ItemPostBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class PostAdapter(
    private val currentUserId: String?,
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(
        private val binding: ItemPostBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            binding.apply {
                // Post info
                tvTitle.text = post.title
                tvContent.text = post.content
                tvCourse.text = post.courseTag
                tvDifficulty.text = post.difficultyLevel
                tvAuthorName.text = post.authorName
                tvTimestamp.text = formatTimestamp(post.timestamp)

                // Engagement
                tvLikes.text = post.likes.toString()
                tvComments.text = post.commentsCount.toString()

                // Like button state
                val isLiked = currentUserId?.let { post.isLikedByUser(it) } ?: false
                if (isLiked) {
                    btnLike.setIconResource(R.drawable.ic_favorite_filled)
                    btnLike.setIconTintResource(R.color.error)
                    btnLike.setTextColor(root.context.getColor(R.color.error))
                } else {
                    btnLike.setIconResource(R.drawable.ic_favorite_border)
                    btnLike.setIconTintResource(R.color.text_secondary)
                    btnLike.setTextColor(root.context.getColor(R.color.text_secondary))
                }

                // Post image
                if (post.imageUrl != null) {
                    ivPostImage.visibility = View.VISIBLE
                    Picasso.get()
                        .load(post.imageUrl)
                        .placeholder(R.color.background_secondary)
                        .error(R.color.background_secondary)
                        .into(ivPostImage)
                } else {
                    ivPostImage.visibility = View.GONE
                }

                // Author image
                if (post.authorImageUrl != null) {
                    Picasso.get()
                        .load(post.authorImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(ivAuthorImage)
                } else {
                    ivAuthorImage.setImageResource(R.drawable.ic_profile_placeholder)
                }

                // Click listeners
                root.setOnClickListener { onPostClick(post) }
                btnLike.setOnClickListener { onLikeClick(post) }
                ivComment.setOnClickListener { onCommentClick(post) }
                tvComments.setOnClickListener { onCommentClick(post) }
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

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
