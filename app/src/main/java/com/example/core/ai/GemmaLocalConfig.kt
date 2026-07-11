package com.example.core.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GemmaModelState {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED,
    FAILED
}

enum class GemmaHardwareAcceleration {
    CPU,
    GPU,
    NPU
}

object GemmaLocalConfig {
    private val _modelState = MutableStateFlow(GemmaModelState.UNINITIALIZED)
    val modelState: StateFlow<GemmaModelState> = _modelState.asStateFlow()

    private val _isModelAwaitingResponse = MutableStateFlow(false)
    val isModelAwaitingResponse: StateFlow<Boolean> = _isModelAwaitingResponse.asStateFlow()

    private val _hardwareAcceleration = MutableStateFlow(GemmaHardwareAcceleration.GPU)
    val hardwareAcceleration: StateFlow<GemmaHardwareAcceleration> = _hardwareAcceleration.asStateFlow()

    // Configurable parameters
    var temperature: Float = 0.4f
    var topP: Float = 0.9f
    var topK: Int = 40
    var maxTokens: Int = 1024
    var simulatedLatencyMs: Long = 1200L
    
    // Safety & test switches to verify error handling / boundaries
    var forceInitializationFailure: Boolean = false
    var forceInferenceTimeout: Boolean = false

    fun setAwaitingResponse(awaiting: Boolean) {
        _isModelAwaitingResponse.value = awaiting
    }

    suspend fun initializeModel(): Boolean {
        if (_modelState.value == GemmaModelState.INITIALIZED) return true
        
        _modelState.value = GemmaModelState.INITIALIZING
        // Simulate hardware resource allocation and model weights loading
        delay(1500L) 
        
        return if (forceInitializationFailure) {
            _modelState.value = GemmaModelState.FAILED
            false
        } else {
            _modelState.value = GemmaModelState.INITIALIZED
            true
        }
    }

    fun setHardware(acceleration: GemmaHardwareAcceleration) {
        _hardwareAcceleration.value = acceleration
    }

    fun resetState() {
        _modelState.value = GemmaModelState.UNINITIALIZED
    }
}
