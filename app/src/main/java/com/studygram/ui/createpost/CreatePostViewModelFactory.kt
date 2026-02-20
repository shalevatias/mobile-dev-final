package com.studygram.ui.createpost

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studygram.data.repository.PostRepository
import com.studygram.utils.PreferenceManager

class CreatePostViewModelFactory(
    private val postRepository: PostRepository,
    private val preferenceManager: PreferenceManager,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePostViewModel::class.java)) {
            return CreatePostViewModel(postRepository, preferenceManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
