package com.studygram.data.remote

import com.studygram.data.model.Quote
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API interface for Quotable API
 * Base URL: https://api.quotable.io/
 */
interface QuoteApiService {

    /**
     * Get a random quote
     * @param maxLength Maximum length of the quote (optional)
     * @param tags Filter quotes by tags (optional, comma-separated)
     * @return Random quote
     */
    @GET("random")
    suspend fun getRandomQuote(
        @Query("maxLength") maxLength: Int? = 150,
        @Query("tags") tags: String? = null
    ): Response<Quote>
}
