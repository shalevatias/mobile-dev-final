package com.studygram.ui.comments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studygram.data.local.CommentDao
import com.studygram.data.local.PostDao
import com.studygram.data.remote.FirestoreManager
import com.studygram.data.repository.CommentRepository

class CommentsViewModelFactory(
    private val context: Context,
    private val commentDao: CommentDao,
    private val postDao: PostDao,
    private val firestoreManager: FirestoreManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommentsViewModel::class.java)) {
            val commentRepository = CommentRepository(
                context,
                commentDao,
                postDao,
                firestoreManager
            )
            return CommentsViewModel(commentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
