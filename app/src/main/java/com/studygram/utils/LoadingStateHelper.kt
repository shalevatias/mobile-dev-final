package com.studygram.utils

import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * Helper class for managing loading states in UI
 * Provides consistent loading behavior across the app
 */
object LoadingStateHelper {

    /**
     * Show loading state for a button
     * Disables button and optionally shows progress
     */
    fun showButtonLoading(button: Button, progressBar: ProgressBar? = null) {
        button.isEnabled = false
        button.alpha = 0.5f
        progressBar?.visible()
    }

    /**
     * Hide loading state for a button
     * Enables button and hides progress
     */
    fun hideButtonLoading(button: Button, progressBar: ProgressBar? = null) {
        button.isEnabled = true
        button.alpha = 1.0f
        progressBar?.gone()
    }

    /**
     * Show loading state for SwipeRefreshLayout
     */
    fun showSwipeRefreshLoading(swipeRefresh: SwipeRefreshLayout) {
        swipeRefresh.isRefreshing = true
    }

    /**
     * Hide loading state for SwipeRefreshLayout
     */
    fun hideSwipeRefreshLoading(swipeRefresh: SwipeRefreshLayout) {
        swipeRefresh.isRefreshing = false
    }

    /**
     * Show full screen loading
     */
    fun showFullScreenLoading(
        contentView: View,
        loadingView: View
    ) {
        contentView.gone()
        loadingView.visible()
    }

    /**
     * Hide full screen loading
     */
    fun hideFullScreenLoading(
        contentView: View,
        loadingView: View
    ) {
        contentView.visible()
        loadingView.gone()
    }

    /**
     * Show overlay loading (dims content)
     */
    fun showOverlayLoading(
        overlayView: View,
        progressBar: ProgressBar
    ) {
        overlayView.visible()
        overlayView.alpha = 0.7f
        progressBar.visible()
    }

    /**
     * Hide overlay loading
     */
    fun hideOverlayLoading(
        overlayView: View,
        progressBar: ProgressBar
    ) {
        overlayView.gone()
        progressBar.gone()
    }

    /**
     * Show inline loading (within a view)
     */
    fun showInlineLoading(
        contentView: View,
        progressBar: ProgressBar,
        errorView: View? = null
    ) {
        contentView.gone()
        progressBar.visible()
        errorView?.gone()
    }

    /**
     * Show content (hide loading)
     */
    fun showContent(
        contentView: View,
        progressBar: ProgressBar,
        errorView: View? = null
    ) {
        contentView.visible()
        progressBar.gone()
        errorView?.gone()
    }

    /**
     * Show error (hide loading and content)
     */
    fun showError(
        contentView: View,
        progressBar: ProgressBar,
        errorView: View
    ) {
        contentView.gone()
        progressBar.gone()
        errorView.visible()
    }

    /**
     * Handle Resource state for a view with content, loading, and error
     */
    fun <T> handleResourceState(
        resource: Resource<T>,
        contentView: View,
        progressBar: ProgressBar,
        errorView: View? = null,
        onSuccess: (T?) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        when (resource) {
            is Resource.Loading -> {
                showInlineLoading(contentView, progressBar, errorView)
            }
            is Resource.Success -> {
                showContent(contentView, progressBar, errorView)
                onSuccess(resource.data)
            }
            is Resource.Error -> {
                if (errorView != null) {
                    showError(contentView, progressBar, errorView)
                } else {
                    showContent(contentView, progressBar, errorView)
                }
                onError(resource.message ?: "An error occurred")
            }
        }
    }

    /**
     * Show empty state
     */
    fun showEmptyState(
        contentView: View,
        emptyView: View,
        progressBar: ProgressBar? = null
    ) {
        contentView.gone()
        emptyView.visible()
        progressBar?.gone()
    }

    /**
     * Hide empty state
     */
    fun hideEmptyState(
        contentView: View,
        emptyView: View
    ) {
        contentView.visible()
        emptyView.gone()
    }
}
