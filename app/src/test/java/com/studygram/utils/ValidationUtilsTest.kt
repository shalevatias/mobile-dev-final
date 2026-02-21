package com.studygram.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ValidationUtils
 */
class ValidationUtilsTest {

    @Test
    fun `validateEmail with valid email returns valid`() {
        val result = ValidationUtils.validateEmail("test@example.com")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateEmail with null returns invalid`() {
        val result = ValidationUtils.validateEmail(null)
        assertFalse(result.isValid)
        assertEquals("Email is required", result.errorMessage)
    }

    @Test
    fun `validateEmail with empty string returns invalid`() {
        val result = ValidationUtils.validateEmail("")
        assertFalse(result.isValid)
        assertEquals("Email is required", result.errorMessage)
    }

    @Test
    fun `validateEmail with invalid format returns invalid`() {
        val result = ValidationUtils.validateEmail("invalid-email")
        assertFalse(result.isValid)
        assertEquals("Please enter a valid email address", result.errorMessage)
    }

    @Test
    fun `validateEmail with missing @ returns invalid`() {
        val result = ValidationUtils.validateEmail("testexample.com")
        assertFalse(result.isValid)
        assertEquals("Please enter a valid email address", result.errorMessage)
    }

    @Test
    fun `validateEmail with missing domain returns invalid`() {
        val result = ValidationUtils.validateEmail("test@")
        assertFalse(result.isValid)
        assertEquals("Please enter a valid email address", result.errorMessage)
    }

    @Test
    fun `validatePassword with valid password returns valid`() {
        val result = ValidationUtils.validatePassword("password123", 6)
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validatePassword with null returns invalid`() {
        val result = ValidationUtils.validatePassword(null, 6)
        assertFalse(result.isValid)
        assertEquals("Password is required", result.errorMessage)
    }

    @Test
    fun `validatePassword with empty string returns invalid`() {
        val result = ValidationUtils.validatePassword("", 6)
        assertFalse(result.isValid)
        assertEquals("Password is required", result.errorMessage)
    }

    @Test
    fun `validatePassword with short password returns invalid`() {
        val result = ValidationUtils.validatePassword("12345", 6)
        assertFalse(result.isValid)
        assertEquals("Password must be at least 6 characters", result.errorMessage)
    }

    @Test
    fun `validatePassword with exact minimum length returns valid`() {
        val result = ValidationUtils.validatePassword("123456", 6)
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateUsername with valid username returns valid`() {
        val result = ValidationUtils.validateUsername("testuser")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateUsername with null returns invalid`() {
        val result = ValidationUtils.validateUsername(null)
        assertFalse(result.isValid)
        assertEquals("Username is required", result.errorMessage)
    }

    @Test
    fun `validateUsername with short username returns invalid`() {
        val result = ValidationUtils.validateUsername("ab")
        assertFalse(result.isValid)
        assertEquals("Username must be at least 3 characters", result.errorMessage)
    }

    @Test
    fun `validatePostTitle with valid title returns valid`() {
        val result = ValidationUtils.validatePostTitle("Valid Title")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validatePostTitle with null returns invalid`() {
        val result = ValidationUtils.validatePostTitle(null)
        assertFalse(result.isValid)
        assertEquals("Title is required", result.errorMessage)
    }

    @Test
    fun `validatePostTitle with short title returns invalid`() {
        val result = ValidationUtils.validatePostTitle("ab")
        assertFalse(result.isValid)
        assertEquals("Title must be at least 3 characters", result.errorMessage)
    }

    @Test
    fun `validatePostTitle with long title returns invalid`() {
        val longTitle = "a".repeat(101)
        val result = ValidationUtils.validatePostTitle(longTitle)
        assertFalse(result.isValid)
        assertEquals("Title must be less than 100 characters", result.errorMessage)
    }

    @Test
    fun `validatePostContent with valid content returns valid`() {
        val result = ValidationUtils.validatePostContent("This is valid content with more than 10 characters")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validatePostContent with null returns invalid`() {
        val result = ValidationUtils.validatePostContent(null)
        assertFalse(result.isValid)
        assertEquals("Content is required", result.errorMessage)
    }

    @Test
    fun `validatePostContent with short content returns invalid`() {
        val result = ValidationUtils.validatePostContent("short")
        assertFalse(result.isValid)
        assertEquals("Content must be at least 10 characters", result.errorMessage)
    }

    @Test
    fun `validateCourseTag with valid course returns valid`() {
        val result = ValidationUtils.validateCourseTag("CS101")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateCourseTag with null returns invalid`() {
        val result = ValidationUtils.validateCourseTag(null)
        assertFalse(result.isValid)
        assertEquals("Course is required", result.errorMessage)
    }

    @Test
    fun `validateCourseTag with short course returns invalid`() {
        val result = ValidationUtils.validateCourseTag("a")
        assertFalse(result.isValid)
        assertEquals("Course name must be at least 2 characters", result.errorMessage)
    }

    @Test
    fun `validateComment with valid comment returns valid`() {
        val result = ValidationUtils.validateComment("Nice post!")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateComment with null returns invalid`() {
        val result = ValidationUtils.validateComment(null)
        assertFalse(result.isValid)
        assertEquals("Comment cannot be empty", result.errorMessage)
    }

    @Test
    fun `validateRequired with value returns valid`() {
        val result = ValidationUtils.validateRequired("value", "Field")
        assertTrue(result.isValid)
        assertNull(result.errorMessage)
    }

    @Test
    fun `validateRequired with null returns invalid`() {
        val result = ValidationUtils.validateRequired(null, "Field")
        assertFalse(result.isValid)
        assertEquals("Field is required", result.errorMessage)
    }
}
