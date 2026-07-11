package com.example.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DiagnosticLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val feature: String,
    val requestSummary: String,
    val isSuccess: Boolean,
    val responseSummary: String?,
    val errorMessage: String?,
    val durationMs: Long,
    val payloadStructure: String? = null,
    val apiResponseErrors: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
}

object APIDiagnosticLogger {
    private val _logs = MutableStateFlow<List<DiagnosticLog>>(emptyList())
    val logs: StateFlow<List<DiagnosticLog>> = _logs.asStateFlow()

    @Synchronized
    fun logCall(
        feature: String,
        requestSummary: String,
        isSuccess: Boolean,
        responseSummary: String?,
        errorMessage: String?,
        durationMs: Long,
        payloadStructure: String? = null,
        apiResponseErrors: String? = null
    ) {
        val newLog = DiagnosticLog(
            feature = feature,
            requestSummary = requestSummary,
            isSuccess = isSuccess,
            responseSummary = responseSummary,
            errorMessage = errorMessage,
            durationMs = durationMs,
            payloadStructure = payloadStructure,
            apiResponseErrors = apiResponseErrors
        )
        val currentList = _logs.value.toMutableList()
        currentList.add(0, newLog) // Add to top of list
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex) // Keep latest 50 logs
        }
        _logs.value = currentList

        // Console printing for immediate developer visibility
        val status = if (isSuccess) "SUCCESS" else "FAILED"
        val errorMsg = if (errorMessage != null) " - Error: $errorMessage" else ""
        println("[API_DIAG_LOG] [$status] Feature: $feature, Duration: ${durationMs}ms$errorMsg")
        println("[API_DIAG_LOG] Request Summary: $requestSummary")
        if (payloadStructure != null) {
            println("[API_DIAG_LOG] Payload Structure: $payloadStructure")
        }
        if (apiResponseErrors != null) {
            println("[API_DIAG_LOG] API Response Errors: $apiResponseErrors")
        }
        if (isSuccess && responseSummary != null) {
            println("[API_DIAG_LOG] Response Preview: ${responseSummary.take(200)}")
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
