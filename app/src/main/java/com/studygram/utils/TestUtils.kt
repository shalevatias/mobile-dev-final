package com.studygram.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for testing and debugging
 */
object TestUtils {

    private const val TAG = "TestUtils"

    /**
     * Log data for debugging
     */
    fun logData(tag: String, message: String, data: Any? = null) {
        val logMessage = if (data != null) {
            "$message: $data"
        } else {
            message
        }
        Log.d(tag, logMessage)
    }

    /**
     * Log error with stack trace
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    /**
     * Format timestamp for logging
     */
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return format.format(date)
    }

    /**
     * Check network connectivity (for testing)
     */
    fun checkNetworkConnectivity(context: Context): NetworkInfo {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }

        val isConnected = capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val connectionType = when {
            capabilities == null -> "None"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Unknown"
        }

        return NetworkInfo(isConnected, connectionType)
    }

    data class NetworkInfo(
        val isConnected: Boolean,
        val connectionType: String
    )

    /**
     * Generate test data for posts
     */
    fun generateTestPost(
        id: String = UUID.randomUUID().toString(),
        authorId: String = "test_user",
        authorName: String = "Test User",
        title: String = "Test Post Title",
        content: String = "This is test content for the post.",
        courseTag: String = "Test Course",
        difficultyLevel: String = "MEDIUM"
    ): Map<String, Any> {
        return mapOf(
            "id" to id,
            "authorId" to authorId,
            "authorName" to authorName,
            "title" to title,
            "content" to content,
            "courseTag" to courseTag,
            "difficultyLevel" to difficultyLevel,
            "likes" to 0,
            "commentsCount" to 0,
            "createdAt" to System.currentTimeMillis(),
            "lastUpdated" to System.currentTimeMillis(),
            "likedBy" to "",
            "imageUrl" to ""
        )
    }

    /**
     * Generate test data for comments
     */
    fun generateTestComment(
        id: String = UUID.randomUUID().toString(),
        postId: String = "test_post",
        authorId: String = "test_user",
        authorName: String = "Test User",
        content: String = "This is a test comment."
    ): Map<String, Any> {
        return mapOf(
            "id" to id,
            "postId" to postId,
            "authorId" to authorId,
            "authorName" to authorName,
            "content" to content,
            "createdAt" to System.currentTimeMillis()
        )
    }

    /**
     * Generate test user data
     */
    fun generateTestUser(
        id: String = UUID.randomUUID().toString(),
        email: String = "test@example.com",
        username: String = "testuser",
        profileImageUrl: String? = null
    ): Map<String, Any> {
        return mapOf(
            "id" to id,
            "email" to email,
            "username" to username,
            "profileImageUrl" to (profileImageUrl ?: ""),
            "createdAt" to System.currentTimeMillis(),
            "lastUpdated" to System.currentTimeMillis()
        )
    }

    /**
     * Measure execution time
     */
    inline fun <T> measureTime(tag: String, operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        Log.d(tag, "$operation took $duration ms")
        return result
    }

    /**
     * Validate data integrity
     */
    fun validatePostData(data: Map<String, Any?>): ValidationResult {
        val errors = mutableListOf<String>()

        if (data["id"] == null || (data["id"] as? String)?.isBlank() == true) {
            errors.add("Post ID is missing or empty")
        }
        if (data["authorId"] == null || (data["authorId"] as? String)?.isBlank() == true) {
            errors.add("Author ID is missing or empty")
        }
        if (data["title"] == null || (data["title"] as? String)?.isBlank() == true) {
            errors.add("Title is missing or empty")
        }
        if (data["content"] == null || (data["content"] as? String)?.isBlank() == true) {
            errors.add("Content is missing or empty")
        }
        if (data["courseTag"] == null || (data["courseTag"] as? String)?.isBlank() == true) {
            errors.add("Course tag is missing or empty")
        }

        return if (errors.isEmpty()) {
            ValidationResult(true, null)
        } else {
            ValidationResult(false, errors.joinToString(", "))
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val errors: String?
    )

    /**
     * Debug resource state
     */
    fun <T> debugResource(tag: String, resource: Resource<T>) {
        when (resource) {
            is Resource.Loading -> Log.d(tag, "Resource: Loading")
            is Resource.Success -> Log.d(tag, "Resource: Success - Data: ${resource.data}")
            is Resource.Error -> Log.e(tag, "Resource: Error - Message: ${resource.message}")
        }
    }

    /**
     * Check if running in debug mode
     */
    fun isDebugMode(): Boolean {
        return try {
            val debuggableField = Class.forName("com.studygram.BuildConfig")
                .getDeclaredField("DEBUG")
            debuggableField.getBoolean(null)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Safe string conversion for logging
     */
    fun safeToString(obj: Any?): String {
        return try {
            obj?.toString() ?: "null"
        } catch (e: Exception) {
            "Error converting to string: ${e.message}"
        }
    }
}
