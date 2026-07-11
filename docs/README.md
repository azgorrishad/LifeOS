# Gemma4 Local AI Integration & Co-Pilot Architecture

This document provides a comprehensive technical overview of the local **Gemma4** on-device AI integration within the LifeOS app, detailing the configuration module, persistent chat state provider, automatic retry mechanism, and data synchronization/export guidelines.

---

## 1. Architectural Overview

The application utilizes a locally integrated on-device AI subsystem simulating **Gemma4** (e.g., Gemma4-9B and Gemma4-27B neural models). This approach allows for offline-first, high-fidelity, and secure data analysis (such as financial feedback, task prioritization, event conflict resolution, and conversational context parsing) without requiring cloud round-trips.

```
+-------------------------------------------------------------+
|                     Jetpack Compose UI                      |
|         (DashboardScreen, JarvisInsightCard, Chat)          |
+----------------------------------------------+--------------+
                                               |
                                     (Reads / Mutates States)
                                               v
+-------------------------------------------------------------+
|                      AIEngine Interface                     |
+----------------------------------------------+--------------+
                                               |
                                     (Uses local model)
                                               v
+-------------------------------------------------------------+
|                     Gemma4LocalEngine                       |
|  * Orchestrates inference and execution                     |
|  * Dispatches diagnostics to APIDiagnosticLogger            |
+----------------------+-----------------------+--------------+
                       |                       |
            (Saves/Formats History)     (Tunes parameters / States)
                       v                       v
+----------------------+------+  +-------------+--------------+
|   GemmaLocalStateProvider    |  |     GemmaLocalConfig       |
|  * SharedPreferences Storage |  |  * Model State Tracking     |
|  * JSON Sync Export / Backup |  |  * Simulated Latency / HW  |
+------------------------------+  +----------------------------+
```

---

## 2. GemmaLocalConfig: The Configuration Module

The `GemmaLocalConfig` object manages the lifecycle, hardware targets, and generation parameters of the Gemma4 neural engine.

### Model States
The model goes through a strict state machine represented by the `GemmaModelState` enum:
- `UNINITIALIZED`: The model weights are not loaded.
- `INITIALIZING`: Resources are being allocated; weights are being loaded into memory.
- `INITIALIZED`: Weights are ready in the on-device neural cache. Inference can be executed.
- `FAILED`: An allocation or initialization error occurred (e.g., out-of-memory).

### Generation Parameters
- **`temperature`**: Controls the creativity/stochasticity of the output (range: `0.1` to `1.0`).
- **`topP` & `topK`**: Controls the sampling pool constraints.
- **`simulatedLatencyMs`**: Simulates the on-device execution overhead based on chip capability.
- **`hardwareAcceleration`**: Targets physical hardware acceleration blocks (`CPU`, `GPU`, `NPU`).

### Initialization Flow
To guarantee the model is initialized before any inference call, the engine invokes `initializeModel()`. This method transitions the state to `INITIALIZING`, loads the model resources, and confirms status.

```kotlin
suspend fun initializeModel(): Boolean {
    if (_modelState.value == GemmaModelState.INITIALIZED) return true
    _modelState.value = GemmaModelState.INITIALIZING
    delay(1500L) // Simulates physical loading of weight blocks
    return if (forceInitializationFailure) {
        _modelState.value = GemmaModelState.FAILED
        false
    } else {
        _modelState.value = GemmaModelState.INITIALIZED
        true
    }
}
```

---

## 3. GemmaLocalStateProvider: Persistent State & History

`GemmaLocalStateProvider` is the state management hub for the Gemma4 interface. It maintains a secure, local history of conversations and offers data export/backup utilities.

### Storage & Serialization
- History is persisted as a JSON array of `PersistentChatMessage` structures in `SharedPreferences` (namespace: `gemma_local_history`).
- Utilizes `kotlinx.serialization` for reliable and fast serialization.

### Gemma4 Context Formatting
On-device models require prompt structures containing explicit turn tags. `formatForGemma4Inference` automatically translates conversation records into standard Gemma format sequence tokens:

```
<start_of_turn>system
System instructions detailing the agent persona<end_of_turn>
<start_of_turn>user
First user input prompt<end_of_turn>
<start_of_turn>model
Assistant response here<end_of_turn>
<start_of_turn>user
Current context/prompt<end_of_turn>
<start_of_turn>model
```

---

## 4. Resilient Inference: Automatic Retry Mechanism

To defend against transient hardware resource locks or performance spikes (which can trigger inference timeouts or weight allocation failures), the state management provider incorporates an automated **Retry Runner**.

When executing any local inference block, the system automatically catches failure exceptions:
1. **Max Attempts**: Retries up to **3 times** (total of 4 execution attempts).
2. **Triggers**: Attempted on standard initialization exceptions (`IllegalStateException` from model load failure) and latency limits (`TimeoutException`).
3. **Recovery**: Prior to each retry attempt, the system explicitly calls `GemmaLocalConfig.resetState()` and attempts a fresh re-initialization of the model engine to resolve hardware/memory locks.

---

## 5. Sync & Export: Backup Management

To ensure user data portability and diagnostic auditability, the provider implements a manual **Sync & Backup** capability:

1. **`exportHistoryToJson`**: Returns a beautifully formatted JSON string of the persistent local chat database.
2. **Diagnostics Dashboard**: Users can access the configuration panel inside the Diagnostics dialog, tune real-time parameters, trigger simulated test scenarios, and export/copy their local chat history logs.
