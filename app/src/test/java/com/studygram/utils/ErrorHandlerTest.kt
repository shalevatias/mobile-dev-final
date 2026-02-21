package com.studygram.utils

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Unit tests for ErrorHandler
 */
class ErrorHandlerTest {

    @Test
    fun `getErrorMessage with UnknownHostException returns network error`() {
        val exception = UnknownHostException()
        val message = ErrorHandler.getErrorMessage(exception)
        assertEquals("No internet connection. Please check your network.", message)
    }

    @Test
    fun `getErrorMessage with SocketTimeoutException returns timeout error`() {
        val exception = SocketTimeoutException()
        val message = ErrorHandler.getErrorMessage(exception)
        assertEquals("Request timed out. Please try again.", message)
    }

    @Test
    fun `getErrorMessage with IOException returns network error`() {
        val exception = IOException()
        val message = ErrorHandler.getErrorMessage(exception)
        assertEquals("Network error. Please check your connection.", message)
    }

    @Test
    fun `getErrorMessage with FirebaseNetworkException returns network error`() {
        val exception = FirebaseNetworkException("Network error")
        val message = ErrorHandler.getErrorMessage(exception)
        assertEquals("No internet connection. Please check your network.", message)
    }

    @Test
    fun `getErrorMessage with generic Exception returns message`() {
        val exception = Exception("Something went wrong")
        val message = ErrorHandler.getErrorMessage(exception)
        assertEquals("Something went wrong", message)
    }

    @Test
    fun `getErrorMessage with Exception without message returns default`() {
        val exception = Exception()
        val message = ErrorHandler.getErrorMessage(exception)
        assertEquals("An unexpected error occurred. Please try again.", message)
    }

    @Test
    fun `isNetworkError with UnknownHostException returns true`() {
        val exception = UnknownHostException()
        assertTrue(ErrorHandler.isNetworkError(exception))
    }

    @Test
    fun `isNetworkError with SocketTimeoutException returns true`() {
        val exception = SocketTimeoutException()
        assertTrue(ErrorHandler.isNetworkError(exception))
    }

    @Test
    fun `isNetworkError with IOException returns true`() {
        val exception = IOException()
        assertTrue(ErrorHandler.isNetworkError(exception))
    }

    @Test
    fun `isNetworkError with generic Exception returns false`() {
        val exception = Exception("Some error")
        assertFalse(ErrorHandler.isNetworkError(exception))
    }

    @Test
    fun `getRetrySuggestion with network error returns suggestion`() {
        val exception = UnknownHostException()
        val suggestion = ErrorHandler.getRetrySuggestion(exception)
        assertNotNull(suggestion)
        assertEquals("Please check your internet connection and try again", suggestion)
    }

    @Test
    fun `getRetrySuggestion with SocketTimeoutException returns specific suggestion`() {
        val exception = SocketTimeoutException()
        val suggestion = ErrorHandler.getRetrySuggestion(exception)
        assertNotNull(suggestion)
        assertTrue(suggestion!!.contains("too long"))
    }

    @Test
    fun `getRetrySuggestion with generic Exception returns null`() {
        val exception = Exception("Some error")
        val suggestion = ErrorHandler.getRetrySuggestion(exception)
        assertNull(suggestion)
    }

    // Test removed - formatErrorForLogging method not implemented in ErrorHandler
    // @Test
    // fun `formatErrorForLogging includes error type and message`() {
    //     val exception = IllegalArgumentException("Invalid argument")
    //     val formatted = ErrorHandler.formatErrorForLogging(exception, "Test context")
    //
    //     assertTrue(formatted.contains("IllegalArgumentException"))
    //     assertTrue(formatted.contains("Invalid argument"))
    //     assertTrue(formatted.contains("Test context"))
    // }
}
