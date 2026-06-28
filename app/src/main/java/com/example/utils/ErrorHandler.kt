package com.example.utils

import android.util.Log

object ErrorHandler {
    fun handleError(exception: Exception, tag: String = "LifeOSError"): String {
        Log.e(tag, "An error occurred", exception)
        return when (exception) {
            is java.io.IOException -> "Network error. Please check your connection."
            is retrofit2.HttpException -> "Server error. Please try again later."
            is android.database.sqlite.SQLiteException -> "Database error. Please try again."
            else -> "An unexpected error occurred: ${exception.localizedMessage}"
        }
    }
}
