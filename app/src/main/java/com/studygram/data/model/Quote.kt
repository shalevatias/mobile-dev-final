package com.studygram.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for inspirational quotes from external API
 * API: https://api.quotable.io/random
 */
data class Quote(
    @SerializedName("_id")
    val id: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("author")
    val author: String,

    @SerializedName("tags")
    val tags: List<String> = emptyList(),

    @SerializedName("length")
    val length: Int = 0
)
