package com.studygram.utils

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Centralized error handling utility
 * Converts exceptions to user-friendly error messages
 */
object ErrorHandler {

    /**
     * Convert an exception to a user-friendly error message
     * @param exception The exception to handle
     * @return User-friendly error message
     */
    fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            // Network errors
            is UnknownHostException -> "No internet connection. Please check your network."
            is SocketTimeoutException -> "Request timed out. Please try again."
            is IOException -> "Network error. Please check your connection."

            // Firebase Authentication errors
            is FirebaseAuthException -> handleAuthException(exception)

            // Firebase Firestore errors
            is FirebaseFirestoreException -> handleFirestoreException(exception)

            // Firebase Network errors
            is FirebaseNetworkException -> "No internet connection. Please check your network."

            // Generic Firebase errors
            is FirebaseException -> "An error occurred. Please try again."

            // Custom errors with messages
            else -> exception.message ?: "An unexpected error occurred. Please try again."
        }
    }

    /**
     * Handle Firebase Authentication exceptions
     */
    private fun handleAuthException(exception: FirebaseAuthException): String {
        return when (exception.errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address"
            "ERROR_WRONG_PASSWORD" -> "Incorrect password"
            "ERROR_USER_NOT_FOUND" -> "No account found with this email"
            "ERROR_USER_DISABLED" -> "This account has been disabled"
            "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email"
            "ERROR_WEAK_PASSWORD" -> "Password is too weak. Please use a stronger password"
            "ERROR_INVALID_CREDENTIAL" -> "Invalid credentials. Please try again"
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "An account already exists with a different sign-in method"
            else -> "Authentication failed: ${exception.message ?: "Unknown error"}"
        }
    }

    /**
     * Handle Firebase Firestore exceptions
     */
    private fun handleFirestoreException(exception: FirebaseFirestoreException): String {
        return when (exception.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "You don't have permission to perform this action"
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "The requested data was not found"
            FirebaseFirestoreException.Code.ALREADY_EXISTS ->
                "This item already exists"
            FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
                "Too many requests. Please try again later"
            FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "Operation failed. Please refresh and try again"
            FirebaseFirestoreException.Code.ABORTED ->
                "Operation was aborted. Please try again"
            FirebaseFirestoreException.Code.OUT_OF_RANGE ->
                "Invalid data range"
            FirebaseFirestoreException.Code.UNIMPLEMENTED ->
                "This feature is not available"
            FirebaseFirestoreException.Code.INTERNAL ->
                "Internal server error. Please try again"
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Service temporarily unavailable. Please try again"
            FirebaseFirestoreException.Code.DATA_LOSS ->
                "Data loss detected. Please contact support"
            FirebaseFirestoreException.Code.UNAUTHENTICATED ->
                "Please sign in to continue"
            FirebaseFirestoreException.Code.CANCELLED ->
                "Operation was cancelled"
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                "Request timed out. Please try again"
            else -> "Database error: ${exception.message ?: "Unknown error"}"
        }
    }

    /**
     * Determine if an error is a network-related error
     * @param exception The exception to check
     * @return True if network error, false otherwise
     */
    fun isNetworkError(exception: Throwable): Boolean {
        return exception is UnknownHostException ||
                exception is SocketTimeoutException ||
                exception is IOException ||
                exception is FirebaseNetworkException ||
                (exception is FirebaseFirestoreException &&
                        exception.code == FirebaseFirestoreException.Code.UNAVAILABLE)
    }

    /**
     * Determine if an error requires authentication
     * @param exception The exception to check
     * @return True if auth required, false otherwise
     */
    fun requiresAuthentication(exception: Throwable): Boolean {
        return (exception is FirebaseFirestoreException &&
                exception.code == FirebaseFirestoreException.Code.UNAUTHENTICATED) ||
                (exception is FirebaseAuthException &&
                        exception.errorCode == "ERROR_INVALID_USER_TOKEN")
    }

    /**
     * Get a retry suggestion based on the error
     * @param exception The exception
     * @return Suggestion message or null
     */
    fun getRetrySuggestion(exception: Throwable): String? {
        return when {
            isNetworkError(exception) ->
                "Please check your internet connection and try again"
            exception is SocketTimeoutException ->
                "The request is taking too long. Try again with a better connection"
            requiresAuthentication(exception) ->
                "Please sign in again to continue"
            exception is FirebaseAuthException && exception.errorCode == "ERROR_TOO_MANY_REQUESTS" ->
                "Please wait a few minutes before trying again"
            else -> null
        }
    }
}
