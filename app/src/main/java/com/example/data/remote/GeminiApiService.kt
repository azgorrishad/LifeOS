package com.example.data.remote

import com.example.BuildConfig
import com.example.utils.ErrorHandler
import com.example.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null
)

@Serializable
data class Tool(
    val googleSearch: GoogleSearch? = null
)

@Serializable
class GoogleSearch()

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val responseFormat: ResponseFormat? = null
)

@Serializable
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@Serializable
data class ResponseFormatText(
    val mimeType: String,
    val schema: JsonObject? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST
    suspend fun generateContent(
        @retrofit2.http.Url url: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiRepository {
    private fun getModelUrl(modelName: String = "gemini-3.5-flash"): String {
        return "v1beta/models/$modelName:generateContent"
    }

    suspend fun getLifeInsights(tasks: String, expenses: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) {
             return@withContext Result.Error(Exception("Missing API Key"), "Please configure your Gemini API Key in the AI Studio Settings.")
        }
        
        val prompt = "Here are my current tasks:\n$tasks\n\nHere are my recent expenses:\n$expenses\n\nAs Jarvis, a highly advanced and minimal AI LifeOS assistant, give me a very brief (2-3 sentences max) insightful observation or recommendation to optimize my day."
        
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are Jarvis from LifeOS. Provide concise, premium, high-value productivity and financial insights."))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )
        
        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.1-flash-lite"), apiKey, request)
            val insight = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No insights available right now."
            Result.Success(insight)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.handleError(e, "GeminiInsights"))
        }
    }

    suspend fun analyzeHabits(productivityData: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) return@withContext Result.Error(Exception("Missing API Key"), "Missing API Key")

        val prompt = "Based on the following user performance data:\n$productivityData\n\nAnalyze the patterns in productivity, task completion, and goal achievement. Suggest 3-5 new habits for the user to adopt, explaining the potential benefits of each habit in relation to their existing goals and observed performance."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert productivity coach. Provide actionable and specific habit recommendations."))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.5-flash"), apiKey, request)
            val habits = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not analyze habits."
            Result.Success(habits)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.handleError(e, "GeminiHabits"))
        }
    }

    suspend fun resolveSchedulingConflict(conflictDetails: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) return@withContext Result.Error(Exception("Missing API Key"), "Missing API Key")

        val prompt = "A scheduling conflict has been detected:\n$conflictDetails\n\nAnalyze the conflicting events and tasks, considering their priorities, durations, and deadlines. Propose at least two alternative scheduling options that resolve the conflict while minimizing disruption to the user's plan. Explain the trade-offs of each proposed solution."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an intelligent conflict resolution engine for a smart scheduling assistant."))),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.1-pro-preview"), apiKey, request)
            val resolution = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not resolve conflict."
            Result.Success(resolution)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.handleError(e, "GeminiScheduling"))
        }
    }

    suspend fun askJarvis(query: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) return@withContext Result.Error(Exception("Missing API Key"), "Missing API Key")

        val prompt = query

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are Jarvis, a highly advanced LifeOS assistant. Provide concise, premium answers using search data when necessary."))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            tools = listOf(Tool(googleSearch = GoogleSearch()))
        )

        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.5-flash"), apiKey, request)
            val answer = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No answer available."
            Result.Success(answer)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.handleError(e, "GeminiAskJarvis"))
        }
    }
}
