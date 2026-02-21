package com.studygram.utils

import android.util.Patterns

/**
 * Utility class for input validation across the app
 * Provides consistent validation rules and error messages
 */
object ValidationUtils {

    /**
     * Email validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Validate email address
     * @param email Email to validate
     * @return ValidationResult with validity and error message
     */
    fun validateEmail(email: String?): ValidationResult {
        return when {
            email.isNullOrBlank() -> ValidationResult(false, "Email is required")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                ValidationResult(false, "Please enter a valid email address")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validate password
     * @param password Password to validate
     * @param minLength Minimum password length (default: 6)
     * @return ValidationResult with validity and error message
     */
    fun validatePassword(password: String?, minLength: Int = 6): ValidationResult {
        return when {
            password.isNullOrBlank() -> ValidationResult(false, "Password is required")
            password.length < minLength ->
                ValidationResult(false, "Password must be at least $minLength characters")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validate username
     * @param username Username to validate
     * @param minLength Minimum username length (default: 3)
     * @param maxLength Maximum username length (default: 20)
     * @return ValidationResult with validity and error message
     */
    fun validateUsername(username: String?, minLength: Int = 3, maxLength: Int = 20): ValidationResult {
        return when {
            username.isNullOrBlank() -> ValidationResult(false, "Username is required")
            username.length < minLength ->
                ValidationResult(false, "Username must be at least $minLength characters")
            username.length > maxLength ->
                ValidationResult(false, "Username must be at most $maxLength characters")
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) ->
                ValidationResult(false, "Username can only contain letters, numbers, and underscores")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validate post title
     * @param title Title to validate
     * @param minLength Minimum title length (default: 3)
     * @param maxLength Maximum title length (default: 100)
     * @return ValidationResult with validity and error message
     */
    fun validatePostTitle(title: String?, minLength: Int = 3, maxLength: Int = 100): ValidationResult {
        return when {
            title.isNullOrBlank() -> ValidationResult(false, "Title is required")
            title.trim().length < minLength ->
                ValidationResult(false, "Title must be at least $minLength characters")
            title.length > maxLength ->
                ValidationResult(false, "Title must be at most $maxLength characters")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validate post content
     * @param content Content to validate
     * @param minLength Minimum content length (default: 10)
     * @param maxLength Maximum content length (default: 5000)
     * @return ValidationResult with validity and error message
     */
    fun validatePostContent(content: String?, minLength: Int = 10, maxLength: Int = 5000): ValidationResult {
        return when {
            content.isNullOrBlank() -> ValidationResult(false, "Content is required")
            content.trim().length < minLength ->
                ValidationResult(false, "Content must be at least $minLength characters")
            content.length > maxLength ->
                ValidationResult(false, "Content is too long (max $maxLength characters)")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validate course tag
     * @param courseTag Course tag to validate
     * @return ValidationResult with validity and error message
     */
    fun validateCourseTag(courseTag: String?): ValidationResult {
        return when {
            courseTag.isNullOrBlank() -> ValidationResult(false, "Course is required")
            courseTag.length < 2 -> ValidationResult(false, "Course name is too short")
            courseTag.length > 50 -> ValidationResult(false, "Course name is too long")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validate comment text
     * @param comment Comment to validate
     * @param minLength Minimum comment length (default: 1)
     * @param maxLength Maximum comment length (default: 500)
     * @return ValidationResult with validity and error message
     */
    fun validateComment(comment: String?, minLength: Int = 1, maxLength: Int = 500): ValidationResult {
        return when {
            comment.isNullOrBlank() -> ValidationResult(false, "Comment cannot be empty")
            comment.trim().length < minLength ->
                ValidationResult(false, "Comment is too short")
            comment.length > maxLength ->
                ValidationResult(false, "Comment is too long (max $maxLength characters)")
            else -> ValidationResult(true)
        }
    }

    /**
     * Validate required field
     * @param value Value to validate
     * @param fieldName Name of the field for error message
     * @return ValidationResult with validity and error message
     */
    fun validateRequired(value: String?, fieldName: String = "This field"): ValidationResult {
        return if (value.isNullOrBlank()) {
            ValidationResult(false, "$fieldName is required")
        } else {
            ValidationResult(true)
        }
    }

    /**
     * Validate that two strings match (for password confirmation)
     * @param value1 First value
     * @param value2 Second value
     * @param fieldName Name of the field for error message
     * @return ValidationResult with validity and error message
     */
    fun validateMatch(value1: String?, value2: String?, fieldName: String = "Fields"): ValidationResult {
        return if (value1 != value2) {
            ValidationResult(false, "$fieldName do not match")
        } else {
            ValidationResult(true)
        }
    }
}
