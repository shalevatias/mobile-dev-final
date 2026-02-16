package com.studygram.data.repository

import com.studygram.data.model.Quote
import com.studygram.data.remote.RetrofitClient
import com.studygram.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for fetching inspirational quotes from external API
 * Handles API calls and error handling
 */
class QuoteRepository {

    private val apiService = RetrofitClient.quoteApiService

    /**
     * Fetch a random inspirational quote
     * Filters for motivational tags related to studying
     * @return Resource with Quote or error
     */
    suspend fun getRandomQuote(): Resource<Quote> = withContext(Dispatchers.IO) {
        try {
            // Try to get quotes with study-related tags first
            val response = apiService.getRandomQuote(
                maxLength = 150,
                tags = "inspirational,wisdom,famous-quotes"
            )

            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                // Fallback: try without tags if specific tags fail
                val fallbackResponse = apiService.getRandomQuote(maxLength = 150)
                if (fallbackResponse.isSuccessful && fallbackResponse.body() != null) {
                    Resource.Success(fallbackResponse.body()!!)
                } else {
                    Resource.Error("Failed to fetch quote: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
