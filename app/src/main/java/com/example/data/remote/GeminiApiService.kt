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
    val googleSearch: JsonObject? = null
)

@Serializable
data class Content(
    val role: String? = null,
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
    val responseFormat: ResponseFormat? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@Serializable
data class ThinkingConfig(
    val thinkingLevel: String
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
        val requestPayload = "GenerateContentRequest(model=gemini-3.5-flash, temperature=0.7f, systemInstruction='Provide concise, high-value productivity and financial insights.')"
        
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) {
             val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateLifeInsightsFallback(tasks, expenses)
             com.example.utils.APIDiagnosticLogger.logCall(
                 feature = "LifeInsights",
                 requestSummary = "Tasks length: ${tasks.length}, Expenses length: ${expenses.length}",
                 isSuccess = false,
                 responseSummary = fallback,
                 errorMessage = "API Key Missing/Unconfigured",
                 durationMs = 0,
                 payloadStructure = requestPayload,
                 apiResponseErrors = "Missing Gemini API key. Local AI Heuristics activated."
             )
             return@withContext Result.Success(fallback)
         }
         
         val prompt = "Here are my current tasks:\n$tasks\n\nHere are my recent expenses:\n$expenses\n\nAnalyze my tasks and expenses to provide spending tips or budget adjustments, and a brief productivity recommendation."
         
         val request = GenerateContentRequest(
             contents = listOf(Content(parts = listOf(Part(text = prompt)))),
             systemInstruction = Content(parts = listOf(Part(text = "Provide concise, high-value productivity and financial insights."))),
             generationConfig = GenerationConfig(temperature = 0.7f)
         )
         
         val startTime = System.currentTimeMillis()
         try {
             val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.5-flash"), apiKey, request)
            val duration = System.currentTimeMillis() - startTime
            val rawInsight = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            val insight = if (rawInsight.isNullOrBlank()) {
                val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateLifeInsightsFallback(tasks, expenses)
                com.example.utils.APIDiagnosticLogger.logCall(
                    feature = "LifeInsights",
                    requestSummary = "Tasks length: ${tasks.length}, Expenses length: ${expenses.length}",
                    isSuccess = false,
                    responseSummary = fallback,
                    errorMessage = "Empty/Safety Blocked Response",
                    durationMs = duration,
                    payloadStructure = requestPayload,
                    apiResponseErrors = "API returned empty or null content. Local AI Heuristics activated."
                )
                fallback
            } else {
                com.example.utils.APIDiagnosticLogger.logCall(
                    feature = "LifeInsights",
                    requestSummary = "Tasks length: ${tasks.length}, Expenses length: ${expenses.length}",
                    isSuccess = true,
                    responseSummary = rawInsight,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = requestPayload
                )
                rawInsight
            }
            Result.Success(insight)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = ErrorHandler.handleError(e, "GemmaInsights")
            val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateLifeInsightsFallback(tasks, expenses)
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "LifeInsights",
                requestSummary = "Tasks length: ${tasks.length}, Expenses length: ${expenses.length}",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = errorMsg,
                durationMs = duration,
                payloadStructure = requestPayload,
                apiResponseErrors = e.stackTraceToString()
            )
            Result.Success(fallback)
        }
    }

    suspend fun analyzeHabits(productivityData: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val requestPayload = "GenerateContentRequest(model=gemini-3.5-flash, temperature=0.7f, systemInstruction='You are an expert productivity coach...')"
        
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) {
            val fallback = "### 🧠 Habit Analytics & Recommendations\n\n" +
                "1. **2-Minute Rule**: If a task takes less than 2 minutes, complete it immediately to clear clutter.\n" +
                "2. **Evening Budget Alignment**: Review and catalog all expenses and incoming funds at 9 PM.\n" +
                "3. **Workspace Organization**: Start work with a 5-minute desk organization to reset focus."
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "AnalyzeHabits",
                requestSummary = "Data length: ${productivityData.length}",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = "API Key Missing/Unconfigured",
                durationMs = 0,
                payloadStructure = requestPayload,
                apiResponseErrors = "Missing Gemini API key. Local AI Heuristics activated."
            )
            return@withContext Result.Success(fallback)
        }

        val prompt = "Based on the following user performance data:\n$productivityData\n\nAnalyze the patterns in productivity, task completion, and goal achievement. Suggest 3-5 new habits for the user to adopt, explaining the potential benefits of each habit in relation to their existing goals and observed performance."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an expert productivity coach. Provide actionable and specific habit recommendations."))),
            generationConfig = GenerationConfig(temperature = 0.7f)
        )

        val startTime = System.currentTimeMillis()
        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.5-flash"), apiKey, request)
            val duration = System.currentTimeMillis() - startTime
            val habits = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not analyze habits."
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "AnalyzeHabits",
                requestSummary = "Data length: ${productivityData.length}",
                isSuccess = true,
                responseSummary = habits,
                errorMessage = null,
                durationMs = duration,
                payloadStructure = requestPayload
            )
            Result.Success(habits)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = ErrorHandler.handleError(e, "GeminiHabits")
            val fallback = "### 🧠 Habit Analytics & Recommendations\n\n" +
                "1. **2-Minute Rule**: If a task takes less than 2 minutes, complete it immediately to clear clutter.\n" +
                "2. **Evening Budget Alignment**: Review and catalog all expenses and incoming funds at 9 PM.\n" +
                "3. **Workspace Organization**: Start work with a 5-minute desk organization to reset focus."
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "AnalyzeHabits",
                requestSummary = "Data length: ${productivityData.length}",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = errorMsg,
                durationMs = duration,
                payloadStructure = requestPayload,
                apiResponseErrors = e.stackTraceToString()
            )
            Result.Success(fallback)
        }
    }

    suspend fun resolveSchedulingConflict(conflictDetails: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val requestPayload = "GenerateContentRequest(model=gemini-3.5-flash, temperature=0.5f, systemInstruction='You are an intelligent conflict resolution engine...')"
        
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) {
            val fallback = "### 🛡️ Scheduling Conflict Resolution\n\n" +
                "1. **Option A (Priority-shift)**: Shift the lower-priority event to 2 hours later to secure your focus.\n" +
                "2. **Option B (Asynchronous Catchup)**: Delegate or record the conflict item to review later."
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "ResolveConflict",
                requestSummary = "Conflict details length: ${conflictDetails.length}",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = "API Key Missing/Unconfigured",
                durationMs = 0,
                payloadStructure = requestPayload,
                apiResponseErrors = "Missing Gemini API key. Local AI Heuristics activated."
            )
            return@withContext Result.Success(fallback)
        }

        val prompt = "A scheduling conflict has been detected:\n$conflictDetails\n\nAnalyze the conflicting events and tasks, considering their priorities, durations, and deadlines. Propose at least two alternative scheduling options that resolve the conflict while minimizing disruption to the user's plan. Explain the trade-offs of each proposed solution."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an intelligent conflict resolution engine for a smart scheduling assistant."))),
            generationConfig = GenerationConfig(temperature = 0.5f)
        )

        val startTime = System.currentTimeMillis()
        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.5-flash"), apiKey, request)
            val duration = System.currentTimeMillis() - startTime
            val resolution = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Could not resolve conflict."
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "ResolveConflict",
                requestSummary = "Conflict details length: ${conflictDetails.length}",
                isSuccess = true,
                responseSummary = resolution,
                errorMessage = null,
                durationMs = duration,
                payloadStructure = requestPayload
            )
            Result.Success(resolution)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = ErrorHandler.handleError(e, "GeminiScheduling")
            val fallback = "### 🛡️ Scheduling Conflict Resolution\n\n" +
                "1. **Option A (Priority-shift)**: Shift the lower-priority event to 2 hours later to secure your focus.\n" +
                "2. **Option B (Asynchronous Catchup)**: Delegate or record the conflict item to review later."
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "ResolveConflict",
                requestSummary = "Conflict details length: ${conflictDetails.length}",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = errorMsg,
                durationMs = duration,
                payloadStructure = requestPayload,
                apiResponseErrors = e.stackTraceToString()
            )
            Result.Success(fallback)
        }
    }

    suspend fun askJarvis(query: String): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val requestPayload = "GenerateContentRequest(model=gemini-3.5-flash, temperature=0.5f, query='$query')"
        
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) {
            val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateChatFallback(query)
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "AskJarvis",
                requestSummary = "Query: ${query.take(150)}...",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = "API Key Missing/Unconfigured",
                durationMs = 0,
                payloadStructure = requestPayload,
                apiResponseErrors = "Missing Gemini API key. Local AI Heuristics activated."
            )
            return@withContext Result.Success(fallback)
        }

        val prompt = query

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an advanced assistant. Provide concise, premium answers."))),
            generationConfig = GenerationConfig(temperature = 0.5f),
            tools = null
        )

        val startTime = System.currentTimeMillis()
        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.5-flash"), apiKey, request)
            val duration = System.currentTimeMillis() - startTime
            val rawAnswer = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            val answer = if (rawAnswer.isNullOrBlank()) {
                val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateChatFallback(query)
                com.example.utils.APIDiagnosticLogger.logCall(
                    feature = "AskJarvis",
                    requestSummary = "Query: ${query.take(150)}...",
                    isSuccess = false,
                    responseSummary = fallback,
                    errorMessage = "Empty/Safety Blocked Response",
                    durationMs = duration,
                    payloadStructure = requestPayload,
                    apiResponseErrors = "API returned empty or null content. Local AI Heuristics activated."
                )
                fallback
            } else {
                com.example.utils.APIDiagnosticLogger.logCall(
                    feature = "AskJarvis",
                    requestSummary = "Query: ${query.take(150)}...",
                    isSuccess = true,
                    responseSummary = rawAnswer,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = requestPayload
                )
                rawAnswer
            }
            Result.Success(answer)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = ErrorHandler.handleError(e, "GeminiAskJarvis")
            val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateChatFallback(query)
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "AskJarvis",
                requestSummary = "Query: ${query.take(150)}...",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = errorMsg,
                durationMs = duration,
                payloadStructure = requestPayload,
                apiResponseErrors = e.stackTraceToString()
            )
            Result.Success(fallback)
        }
    }

    suspend fun askJarvisChat(history: List<com.example.core.ai.ChatMessage>, useThinking: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val model = if (useThinking) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
        val requestPayload = "GenerateContentRequest(model=$model, useThinking=$useThinking, temperature=0.5f)"
        val lastMessage = history.lastOrNull()?.text ?: ""
        
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) {
            val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateChatFallback(lastMessage)
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "AskJarvisChat(Thinking=$useThinking)",
                requestSummary = "History size: ${history.size}, Last message: ${lastMessage.take(100)}...",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = "API Key Missing/Unconfigured",
                durationMs = 0,
                payloadStructure = requestPayload,
                apiResponseErrors = "Missing Gemini API key. Local AI Heuristics activated."
            )
            return@withContext Result.Success(fallback)
        }

        val contents = history.map { Content(role = it.role, parts = listOf(Part(text = it.text))) }
        val tools = null
        val thinkingConfig = null

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = Content(parts = listOf(Part(text = "You are an advanced assistant. Provide concise, premium answers based on user's life data."))),
            generationConfig = GenerationConfig(
                temperature = 0.5f,
                thinkingConfig = thinkingConfig
            ),
            tools = tools
        )

        val startTime = System.currentTimeMillis()
        try {
            val response = RetrofitClient.service.generateContent(getModelUrl(model), apiKey, request)
            val duration = System.currentTimeMillis() - startTime
            val rawAnswer = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            val answer = if (rawAnswer.isNullOrBlank()) {
                val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateChatFallback(lastMessage)
                com.example.utils.APIDiagnosticLogger.logCall(
                    feature = "AskJarvisChat(Thinking=$useThinking)",
                    requestSummary = "History size: ${history.size}, Last message: ${lastMessage.take(100)}...",
                    isSuccess = false,
                    responseSummary = fallback,
                    errorMessage = "Empty/Safety Blocked Response",
                    durationMs = duration,
                    payloadStructure = requestPayload,
                    apiResponseErrors = "API returned empty or null content. Local AI Heuristics activated."
                )
                fallback
            } else {
                com.example.utils.APIDiagnosticLogger.logCall(
                    feature = "AskJarvisChat(Thinking=$useThinking)",
                    requestSummary = "History size: ${history.size}, Last message: ${lastMessage.take(100)}...",
                    isSuccess = true,
                    responseSummary = rawAnswer,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = requestPayload
                )
                rawAnswer
            }
            Result.Success(answer)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = ErrorHandler.handleError(e, "GeminiAskJarvisChat")
            val fallback = com.example.core.ai.LocalAIHeuristicsEngine.generateChatFallback(lastMessage)
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "AskJarvisChat(Thinking=$useThinking)",
                requestSummary = "History size: ${history.size}, Last message: ${lastMessage.take(100)}...",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = errorMsg,
                durationMs = duration,
                payloadStructure = requestPayload,
                apiResponseErrors = e.stackTraceToString()
            )
            Result.Success(fallback)
        }
    }

    suspend fun getTaskPrioritization(tasks: List<String>): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val requestPayload = "GenerateContentRequest(model=gemini-3.5-flash, temperature=0.4f, tasksCount=${tasks.size})"
        
        if (apiKey.isEmpty() || apiKey.startsWith("MY_GEMINI")) {
            val fallback = "### 🎯 Task Prioritization & Strategy\n\n" +
                "Suggested order of priority:\n\n" +
                tasks.mapIndexed { idx, t -> "${idx + 1}. **$t**" }.joinToString("\n") +
                "\n\n*Strategic Recommendation: Complete your highest priority task first to maximize momentum.*"
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "TaskPrioritization",
                requestSummary = "Tasks count: ${tasks.size}",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = "API Key Missing/Unconfigured",
                durationMs = 0,
                payloadStructure = requestPayload,
                apiResponseErrors = "Missing Gemini API key. Local AI Heuristics activated."
            )
            return@withContext Result.Success(fallback)
        }

        val prompt = "Here are my current tasks:\n" + tasks.joinToString("\n") + "\n\nPlease suggest which tasks I should prioritize today and why. Keep it concise and motivating."

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "You are an intelligent productivity coach."))),
            generationConfig = GenerationConfig(temperature = 0.4f)
        )

        val startTime = System.currentTimeMillis()
        try {
            val response = RetrofitClient.service.generateContent(getModelUrl("gemini-3.5-flash"), apiKey, request)
            val duration = System.currentTimeMillis() - startTime
            val answer = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No prioritization available."
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "TaskPrioritization",
                requestSummary = "Tasks count: ${tasks.size}",
                isSuccess = true,
                responseSummary = answer,
                errorMessage = null,
                durationMs = duration,
                payloadStructure = requestPayload
            )
            Result.Success(answer)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            val errorMsg = ErrorHandler.handleError(e, "GeminiTaskPrioritization")
            val fallback = "### 🎯 Task Prioritization & Strategy\n\n" +
                "Suggested order of priority:\n\n" +
                tasks.mapIndexed { idx, t -> "${idx + 1}. **$t**" }.joinToString("\n") +
                "\n\n*Strategic Recommendation: Complete your highest priority task first to maximize momentum.*"
            com.example.utils.APIDiagnosticLogger.logCall(
                feature = "TaskPrioritization",
                requestSummary = "Tasks count: ${tasks.size}",
                isSuccess = false,
                responseSummary = fallback,
                errorMessage = errorMsg,
                durationMs = duration,
                payloadStructure = requestPayload,
                apiResponseErrors = e.stackTraceToString()
            )
            Result.Success(fallback)
        }
    }
}
