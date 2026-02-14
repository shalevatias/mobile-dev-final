package com.studygram.data.model

enum class DifficultyLevel(val displayName: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    companion object {
        fun fromString(value: String): DifficultyLevel {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
        }
    }
}
