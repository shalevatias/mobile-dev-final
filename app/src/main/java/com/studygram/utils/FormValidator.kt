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
     * Validate that two password fields match
     */
    fun validatePasswordMatch(
        passwordLayout: TextInputLayout,
        confirmPasswordLayout: TextInputLayout
    ): Boolean {
        val password = passwordLayout.editText?.text?.toString()
        val confirmPassword = confirmPasswordLayout.editText?.text?.toString()

        val result = ValidationUtils.validateMatch(password, confirmPassword, "Passwords")

        return if (result.isValid) {
            confirmPasswordLayout.error = null
            confirmPasswordLayout.isErrorEnabled = false
            true
        } else {
            confirmPasswordLayout.error = result.errorMessage
            confirmPasswordLayout.isErrorEnabled = true
            result.errorMessage?.let { errors.add(it) }
            false
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

    /**
     * Validate multiple fields at once
     * Returns true if all are valid, false if any are invalid
     */
    fun validateAll(vararg validations: () -> Boolean): Boolean {
        clearErrorList()
        val results = validations.map { it() }
        return results.all { it }
    }

    companion object {
        /**
         * Quick validation without FormValidator instance
         */
        fun quickValidate(
            textInputLayout: TextInputLayout,
            validator: (String?) -> ValidationUtils.ValidationResult
        ): Boolean {
            return FormValidator().validate(textInputLayout, validator)
        }
    }
}

/**
 * Extension function for TextInputLayout to easily validate
 */
fun TextInputLayout.validateWith(validator: (String?) -> ValidationUtils.ValidationResult): Boolean {
    val text = this.editText?.text?.toString()
    val result = validator(text)

    return if (result.isValid) {
        this.error = null
        this.isErrorEnabled = false
        true
    } else {
        this.error = result.errorMessage
        this.isErrorEnabled = true
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
