package com.studygram.utils

import com.google.android.material.textfield.TextInputLayout

/**
 * Helper class for form validation with Material TextInputLayout
 * Provides easy validation with error display
 */
class FormValidator {

    private val errors = mutableListOf<String>()

    /**
     * Validate a TextInputLayout field
     * @param textInputLayout The input layout to validate
     * @param validator Validation function that returns ValidationResult
     * @return True if valid, false otherwise
     */
    fun validate(
        textInputLayout: TextInputLayout,
        validator: (String?) -> ValidationUtils.ValidationResult
    ): Boolean {
        val text = textInputLayout.editText?.text?.toString()
        val result = validator(text)

        return if (result.isValid) {
            textInputLayout.error = null
            textInputLayout.isErrorEnabled = false
            true
        } else {
            textInputLayout.error = result.errorMessage
            textInputLayout.isErrorEnabled = true
            result.errorMessage?.let { errors.add(it) }
            false
        }
    }

    /**
     * Validate email field
     */
    fun validateEmail(textInputLayout: TextInputLayout): Boolean {
        return validate(textInputLayout, ValidationUtils::validateEmail)
    }

    /**
     * Validate password field
     */
    fun validatePassword(textInputLayout: TextInputLayout, minLength: Int = 6): Boolean {
        return validate(textInputLayout) { password ->
            ValidationUtils.validatePassword(password, minLength)
        }
    }

    /**
     * Validate username field
     */
    fun validateUsername(textInputLayout: TextInputLayout): Boolean {
        return validate(textInputLayout, ValidationUtils::validateUsername)
    }

    /**
     * Validate post title field
     */
    fun validatePostTitle(textInputLayout: TextInputLayout): Boolean {
        return validate(textInputLayout, ValidationUtils::validatePostTitle)
    }

    /**
     * Validate post content field
     */
    fun validatePostContent(textInputLayout: TextInputLayout): Boolean {
        return validate(textInputLayout, ValidationUtils::validatePostContent)
    }

    /**
     * Validate course tag field
     */
    fun validateCourseTag(textInputLayout: TextInputLayout): Boolean {
        return validate(textInputLayout, ValidationUtils::validateCourseTag)
    }

    /**
     * Validate comment field
     */
    fun validateComment(textInputLayout: TextInputLayout): Boolean {
        return validate(textInputLayout, ValidationUtils::validateComment)
    }

    /**
     * Validate required field
     */
    fun validateRequired(textInputLayout: TextInputLayout, fieldName: String): Boolean {
        return validate(textInputLayout) { value ->
            ValidationUtils.validateRequired(value, fieldName)
        }
    }

    /**
     * Clear all errors from a TextInputLayout
     */
    fun clearError(textInputLayout: TextInputLayout) {
        textInputLayout.error = null
        textInputLayout.isErrorEnabled = false
    }

    /**
     * Clear all errors from multiple TextInputLayouts
     */
    fun clearErrors(vararg textInputLayouts: TextInputLayout) {
        textInputLayouts.forEach { clearError(it) }
        errors.clear()
    }

    /**
     * Get all validation errors
     */
    fun getErrors(): List<String> = errors.toList()

    /**
     * Get first validation error
     */
    fun getFirstError(): String? = errors.firstOrNull()

    /**
     * Check if there are any errors
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Clear all collected errors
     */
    fun clearErrorList() {
        errors.clear()
    }
}

/**
 * Extension function to validate TextInputLayout with a validator function
 */
private fun TextInputLayout.validateWith(
    validator: (String?) -> ValidationUtils.ValidationResult
): Boolean {
    val text = editText?.text?.toString()
    val result = validator(text)

    return if (result.isValid) {
        error = null
        isErrorEnabled = false
        true
    } else {
        error = result.errorMessage
        isErrorEnabled = true
        false
    }
}

/**
 * Extension functions for common validations
 */
fun TextInputLayout.validateEmail(): Boolean =
    validateWith(ValidationUtils::validateEmail)

fun TextInputLayout.validatePassword(minLength: Int = 6): Boolean =
    validateWith { ValidationUtils.validatePassword(it, minLength) }

fun TextInputLayout.validateUsername(): Boolean =
    validateWith(ValidationUtils::validateUsername)

fun TextInputLayout.validateRequired(fieldName: String): Boolean =
    validateWith { ValidationUtils.validateRequired(it, fieldName) }
