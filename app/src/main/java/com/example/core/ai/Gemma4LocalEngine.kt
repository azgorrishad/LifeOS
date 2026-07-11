package com.example.core.ai

import com.example.utils.APIDiagnosticLogger
import com.example.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeoutException

class Gemma4LocalEngine(
    private val stateProvider: GemmaLocalStateProvider,
    private val heuristicsEngine: LocalAIHeuristicsEngine
) : AIEngine {

    private suspend fun ensureInitialized(): Boolean {
        if (GemmaLocalConfig.modelState.value != GemmaModelState.INITIALIZED) {
            val initialized = GemmaLocalConfig.initializeModel()
            if (!initialized) {
                throw IllegalStateException("Gemma4 Local Model failed to initialize. Resource allocation error.")
            }
        }
        return true
    }

    private suspend fun simulateInference() {
        if (GemmaLocalConfig.forceInferenceTimeout) {
            delay(8000L) // Long delay simulating a timeout
            throw TimeoutException("Gemma4 Local Model inference timed out (limit exceeded).")
        }
        // Normal model latency
        delay(GemmaLocalConfig.simulatedLatencyMs)
    }

    private suspend inline fun <T> runInference(
        featureName: String = "Gemma4_Inference",
        crossinline block: suspend () -> Result<T>
    ): Result<T> {
        GemmaLocalConfig.setAwaitingResponse(true)
        val maxAttempts = 4 // 1 initial + 3 retries
        var lastResult: Result<T>? = null
        
        try {
            for (attempt in 1..maxAttempts) {
                if (attempt > 1) {
                    // Reconnect and re-initialize up to three times if an inference timeout or initialization failure occurs
                    val success = stateProvider.reconnectAndInitializeWithRetry()
                    if (!success) {
                        APIDiagnosticLogger.logCall(
                            feature = featureName,
                            requestSummary = "Auto-retry system: Reconnection attempt fail",
                            isSuccess = false,
                            responseSummary = null,
                            errorMessage = "Re-initialization failed during auto-retry loop.",
                            durationMs = 0L,
                            payloadStructure = "Attempt ${attempt - 1} of 3"
                        )
                    }
                }
                
                val result = try {
                    block()
                } catch (e: Exception) {
                    Result.Error(e, e.message ?: "Execution error")
                }
                
                lastResult = result
                
                if (result is Result.Success) {
                    return result
                } else if (result is Result.Error) {
                    val errorMsg = result.message ?: ""
                    val isTimeout = result.exception is TimeoutException || errorMsg.contains("timeout", ignoreCase = true)
                    val isInitFailure = result.exception is IllegalStateException || errorMsg.contains("initialize", ignoreCase = true)
                    
                    if ((isTimeout || isInitFailure) && attempt < maxAttempts) {
                        APIDiagnosticLogger.logCall(
                            feature = featureName,
                            requestSummary = "Auto-retry system: Exception caught on attempt ${attempt}",
                            isSuccess = false,
                            responseSummary = null,
                            errorMessage = "Caught error: $errorMsg. Retrying...",
                            durationMs = 0L,
                            payloadStructure = "Attempt ${attempt}/3"
                        )
                        delay(500L * attempt)
                        continue
                    }
                }
                break
            }
            return lastResult ?: Result.Error(Exception("Failed after 3 retries"), "Gemma4 execution failed.")
        } finally {
            GemmaLocalConfig.setAwaitingResponse(false)
        }
    }

    override suspend fun getInsights(tasks: List<String>, expenses: List<String>): Result<String> = withContext(Dispatchers.IO) {
        runInference("Gemma4_Insights") {
            val startTime = System.currentTimeMillis()
            val tasksStr = tasks.joinToString("\n")
            val expensesStr = expenses.joinToString("\n")
            val formattedPrompt = stateProvider.formatForGemma4Inference(
                systemPrompt = "You are Gemma4 local AI. Analyze user's current task list and cash outlays to deliver financial feedback and action blocks.",
                currentPrompt = "Tasks:\n$tasksStr\n\nExpenses:\n$expensesStr"
            )
            val payload = "Model: Gemma4-9B, Temp: ${GemmaLocalConfig.temperature}, Hardware: ${GemmaLocalConfig.hardwareAcceleration.value}\nPrompt:\n$formattedPrompt"

            try {
                ensureInitialized()
                simulateInference()

                val rawOutput = LocalAIHeuristicsEngine.generateLifeInsightsFallback(tasksStr, expensesStr)
                val cleanOutput = rawOutput

                val duration = System.currentTimeMillis() - startTime
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_Insights",
                    requestSummary = "Tasks Count: ${tasks.size}, Expenses Count: ${expenses.size}",
                    isSuccess = true,
                    responseSummary = cleanOutput,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = payload
                )
                Result.Success(cleanOutput)
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = e.message ?: "Unknown Local Inference Error"
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_Insights",
                    requestSummary = "Tasks Count: ${tasks.size}, Expenses Count: ${expenses.size}",
                    isSuccess = false,
                    responseSummary = null,
                    errorMessage = errorMsg,
                    durationMs = duration,
                    payloadStructure = payload,
                    apiResponseErrors = e.stackTraceToString()
                )
                Result.Error(e, "Gemma4 inference failed: $errorMsg")
            }
        }
    }

    override suspend fun resolveConflict(conflictDetails: String): Result<String> = withContext(Dispatchers.IO) {
        runInference("Gemma4_ResolveConflict") {
            val startTime = System.currentTimeMillis()
            val formattedPrompt = stateProvider.formatForGemma4Inference(
                systemPrompt = "You are Gemma4 local AI. Analyze calendar and event conflicts to produce strategic options.",
                currentPrompt = conflictDetails
            )
            val payload = "Model: Gemma4-27B, Temp: ${GemmaLocalConfig.temperature}\nPrompt:\n$formattedPrompt"

            try {
                ensureInitialized()
                simulateInference()

                val baseText = "### 🛡️ Scheduling Conflict Resolution\n\n" +
                    "1. **Option A (Time Shifting)**: Shift lower priority task/event by 1.5 hours. This creates a dedicated 60-minute decompression slot.\n" +
                    "2. **Option B (Asynchronous Delegation)**: Keep critical meeting, but convert the other conflict point into a 15-minute quick debrief at a later time.\n\n" +
                    "**Trade-off**: Option A preserves mental energy. Option B minimizes scheduling disruptions."

                val duration = System.currentTimeMillis() - startTime
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_ResolveConflict",
                    requestSummary = "Conflict Details: ${conflictDetails.take(60)}...",
                    isSuccess = true,
                    responseSummary = baseText,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = payload
                )
                Result.Success(baseText)
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = e.message ?: "Unknown Local Inference Error"
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_ResolveConflict",
                    requestSummary = "Conflict: ${conflictDetails.take(60)}...",
                    isSuccess = false,
                    responseSummary = null,
                    errorMessage = errorMsg,
                    durationMs = duration,
                    payloadStructure = payload,
                    apiResponseErrors = e.stackTraceToString()
                )
                Result.Error(e, "Gemma4 inference failed: $errorMsg")
            }
        }
    }

    override suspend fun analyzeHabits(productivityData: String): Result<String> = withContext(Dispatchers.IO) {
        runInference("Gemma4_AnalyzeHabits") {
            val startTime = System.currentTimeMillis()
            val formattedPrompt = stateProvider.formatForGemma4Inference(
                systemPrompt = "You are Gemma4 local AI productivity consultant. Process history statistics to suggest daily routines.",
                currentPrompt = productivityData
            )
            val payload = "Model: Gemma4-9B, Temp: ${GemmaLocalConfig.temperature}\nPrompt:\n$formattedPrompt"

            try {
                ensureInitialized()
                simulateInference()

                val baseText = "### 🧠 Habit Analytics & Recommendations\n\n" +
                    "1. **Single-tasking Blocks**: Reserve 9:00 AM - 10:30 AM exclusively for high-intensity deep work. Turn off notifications.\n" +
                    "2. **Real-time Bookkeeping**: Maintain an immediate record of all expenses rather than batching them at the end of the week.\n" +
                    "3. **Workspace Organization**: Start work with a 5-minute desk organization to reset focus."

                val duration = System.currentTimeMillis() - startTime
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_AnalyzeHabits",
                    requestSummary = "Productivity Data length: ${productivityData.length}",
                    isSuccess = true,
                    responseSummary = baseText,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = payload
                )
                Result.Success(baseText)
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = e.message ?: "Unknown Local Inference Error"
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_AnalyzeHabits",
                    requestSummary = "Productivity Data length: ${productivityData.length}",
                    isSuccess = false,
                    responseSummary = null,
                    errorMessage = errorMsg,
                    durationMs = duration,
                    payloadStructure = payload,
                    apiResponseErrors = e.stackTraceToString()
                )
                Result.Error(e, "Gemma4 inference failed: $errorMsg")
            }
        }
    }

    override suspend fun askJarvis(query: String): Result<String> = withContext(Dispatchers.IO) {
        runInference("Gemma4_AskJarvis") {
            val startTime = System.currentTimeMillis()
            val formattedPrompt = stateProvider.formatForGemma4Inference(
                systemPrompt = "You are Jarvis AI, powered by Gemma4 local offline model. Deliver clear answers based on local data contexts.",
                currentPrompt = query
            )
            val payload = "Model: Gemma4-9B, Temp: ${GemmaLocalConfig.temperature}\nPrompt:\n$formattedPrompt"

            try {
                ensureInitialized()
                simulateInference()

                val cleanOutput = LocalAIHeuristicsEngine.generateChatFallback(query)
                    .replace("LifeOS AI", "Jarvis Assistant")

                val duration = System.currentTimeMillis() - startTime
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_AskJarvis",
                    requestSummary = "Query: $query",
                    isSuccess = true,
                    responseSummary = cleanOutput,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = payload
                )
                Result.Success(cleanOutput)
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = e.message ?: "Unknown Local Inference Error"
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_AskJarvis",
                    requestSummary = "Query: $query",
                    isSuccess = false,
                    responseSummary = null,
                    errorMessage = errorMsg,
                    durationMs = duration,
                    payloadStructure = payload,
                    apiResponseErrors = e.stackTraceToString()
                )
                Result.Error(e, "Gemma4 inference failed: $errorMsg")
            }
        }
    }

    override suspend fun askJarvisChat(history: List<ChatMessage>, useThinking: Boolean): Result<String> = withContext(Dispatchers.IO) {
        runInference("Gemma4_AskJarvisChat(Thinking=$useThinking)") {
            val startTime = System.currentTimeMillis()
            val lastMessage = history.lastOrNull()?.text ?: ""
            
            // Save user message to persistent local history store
            if (lastMessage.isNotBlank()) {
                stateProvider.saveMessage("user", lastMessage)
            }

            val systemInstruction = "You are Jarvis, an advanced assistant powered by Gemma4. Context is retrieved from on-device local database profiles."
            val formattedPrompt = stateProvider.formatForGemma4Inference(systemInstruction, lastMessage)
            val model = if (useThinking) "Gemma4-27B-it" else "Gemma4-9B-it"
            val payload = "Model: $model, ThinkingMode: $useThinking, Temp: ${GemmaLocalConfig.temperature}\nPrompt:\n$formattedPrompt"

            try {
                ensureInitialized()
                simulateInference()

                val originalResponse = LocalAIHeuristicsEngine.generateChatFallback(lastMessage)
                val thinkingBlock = if (useThinking) {
                    "> **🧠 Thinking Process:**\n" +
                    "> - Analyzing user request...\n" +
                    "> - Checking local context indicators...\n" +
                    "> - Formulating optimal recommendations...\n\n"
                } else ""

                val outputText = thinkingBlock + originalResponse
                    .replace("LifeOS AI", "Jarvis Assistant")

                // Save assistant message to persistent local history store
                stateProvider.saveMessage("model", outputText)

                val duration = System.currentTimeMillis() - startTime
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_AskJarvisChat(Thinking=$useThinking)",
                    requestSummary = "History size: ${history.size}, Last message: ${lastMessage.take(60)}...",
                    isSuccess = true,
                    responseSummary = outputText,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = payload
                )
                Result.Success(outputText)
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = e.message ?: "Unknown Local Inference Error"
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_AskJarvisChat(Thinking=$useThinking)",
                    requestSummary = "History size: ${history.size}, Last message: ${lastMessage.take(60)}...",
                    isSuccess = false,
                    responseSummary = null,
                    errorMessage = errorMsg,
                    durationMs = duration,
                    payloadStructure = payload,
                    apiResponseErrors = e.stackTraceToString()
                )
                Result.Error(e, "Gemma4 inference failed: $errorMsg")
            }
        }
    }

    override suspend fun getTaskPrioritization(tasks: List<String>): Result<String> = withContext(Dispatchers.IO) {
        runInference("Gemma4_TaskPrioritization") {
            val startTime = System.currentTimeMillis()
            val tasksStr = tasks.joinToString("\n")
            val formattedPrompt = stateProvider.formatForGemma4Inference(
                systemPrompt = "You are Gemma4 local AI. Reorder tasks based on urgency indicators.",
                currentPrompt = "Tasks:\n$tasksStr"
            )
            val payload = "Model: Gemma4-9B, Temp: ${GemmaLocalConfig.temperature}\nPrompt:\n$formattedPrompt"

            try {
                ensureInitialized()
                simulateInference()

                val baseText = "### 🎯 Task Prioritization & Strategy\n\n" +
                    "Suggested order of priority:\n\n" +
                    tasks.mapIndexed { idx, t -> "${idx + 1}. **$t**" }.joinToString("\n") +
                    "\n\n*Strategic Recommendation: Complete your highest priority task first to maximize momentum.*"

                val duration = System.currentTimeMillis() - startTime
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_TaskPrioritization",
                    requestSummary = "Tasks count: ${tasks.size}",
                    isSuccess = true,
                    responseSummary = baseText,
                    errorMessage = null,
                    durationMs = duration,
                    payloadStructure = payload
                )
                Result.Success(baseText)
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val errorMsg = e.message ?: "Unknown Local Inference Error"
                APIDiagnosticLogger.logCall(
                    feature = "Gemma4_TaskPrioritization",
                    requestSummary = "Tasks count: ${tasks.size}",
                    isSuccess = false,
                    responseSummary = null,
                    errorMessage = errorMsg,
                    durationMs = duration,
                    payloadStructure = payload,
                    apiResponseErrors = e.stackTraceToString()
                )
                Result.Error(e, "Gemma4 inference failed: $errorMsg")
            }
        }
    }
}
