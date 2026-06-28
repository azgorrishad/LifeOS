package com.example.utils

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String = exception.localizedMessage ?: "Unknown Error") : Result<Nothing>()
    object Loading : Result<Nothing>()
}
