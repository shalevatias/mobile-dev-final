package com.studygram.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.studygram.R
import com.studygram.data.model.Post
import com.studygram.databinding.ItemPostBinding
import com.studygram.utils.loadImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val currentUserId: String?
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
                tvAuthorName.text = post.authorName
                tvTitle.text = post.title
                tvContent.text = post.content
                tvCourse.text = post.courseTag
                tvDifficulty.text = post.difficultyLevel
                tvLikes.text = post.likes.toString()
                tvComments.text = post.commentsCount.toString()
                tvTimestamp.text = formatTimestamp(post.timestamp)

                if (post.authorImageUrl != null) {
                    ivAuthorImage.loadImage(post.authorImageUrl, R.drawable.ic_profile_placeholder)
                } else {
                    ivAuthorImage.setImageResource(R.drawable.ic_profile_placeholder)
                }

                if (post.imageUrl != null) {
                    ivPostImage.loadImage(post.imageUrl)
                } else {
                    ivPostImage.setImageResource(0)
                }

                val isLiked = post.isLikedByUser(currentUserId ?: "")
                btnLike.setIconResource(
                    if (isLiked) R.drawable.ic_favorite_filled
                    else R.drawable.ic_favorite_border
                )

                root.setOnClickListener {
                    onPostClick(post)
                }

                btnLike.setOnClickListener {
                    onLikeClick(post)
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

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
