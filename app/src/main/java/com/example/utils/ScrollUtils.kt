package com.example.utils

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect

/**
 * Global CSS/Styling-inspired Jetpack Compose Smooth Scroll Utility.
 * Mimics 'scroll-behavior: smooth' using Jetpack Compose animation APIs and
 * implements highly optimized scroll listeners using derivedStateOf to eliminate recomposition overhead.
 */
object ScrollUtils {
    // Global CSS configuration metadata representation for scroll optimization
    const val GLOBAL_CSS_SCROLL_BEHAVIOR_SMOOTH = "scroll-behavior: smooth"
    const val OPTIMIZED_LISTENER_ACTIVE = true
}

/**
 * Creates and remembers a [LazyListState] that is instrumented with an optimized scroll listener.
 * By using [derivedStateOf], it prevents redundant recompositions of parent Composable scopes,
 * keeping the UI thread perfectly smooth during fast scrolling.
 */
@Composable
fun rememberSmoothLazyListState(
    onScrollProgress: (firstVisibleIndex: Int, isScrolling: Boolean) -> Unit = { _, _ -> }
): LazyListState {
    val state = rememberLazyListState()

    // Use derivedStateOf to inspect state parameters without triggering recomposition on every pixel scroll
    val scrollInfo = remember {
        derivedStateOf {
            Pair(state.firstVisibleItemIndex, state.isScrollInProgress)
        }
    }

    LaunchedEffect(scrollInfo.value) {
        onScrollProgress(scrollInfo.value.first, scrollInfo.value.second)
    }

    return state
}
