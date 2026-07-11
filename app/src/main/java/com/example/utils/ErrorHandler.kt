package com.example.utils

import android.util.Log

object ErrorHandler {
    fun handleError(exception: Exception, tag: String = "LifeOSError"): String {
        try {
            Log.e(tag, "An error occurred", exception)
        } catch (e: RuntimeException) {
            println("[$tag] An error occurred: ${exception.message}")
            exception.printStackTrace()
        }
        return when (exception) {
            is java.io.IOException -> "Network error. Please check your connection."
            is retrofit2.HttpException -> {
                val errorBody = try {
                    exception.response()?.errorBody()?.string()
                } catch (e: Exception) {
                    null
                }
                "Server error (${exception.code()}): ${errorBody ?: exception.message()}"
            }
            is android.database.sqlite.SQLiteException -> "Database error. Please try again."
            else -> "An unexpected error occurred: ${exception.localizedMessage}"
        }
    }
}
