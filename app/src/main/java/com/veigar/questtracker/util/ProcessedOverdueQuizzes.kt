package com.veigar.questtracker.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProcessedOverdueQuizzes(context: Context) {
    
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun addProcessedQuiz(quizId: String) {
        val currentSet = getProcessedQuizzes().toMutableSet()
        currentSet.add(quizId)
        saveProcessedQuizzes(currentSet.toList())
        Log.d("ProcessedOverdueQuizzes", "Added processed quiz: $quizId")
    }
    
    fun isQuizProcessed(quizId: String): Boolean {
        val processedQuizzes = getProcessedQuizzes()
        val isProcessed = processedQuizzes.contains(quizId)
        Log.d("ProcessedOverdueQuizzes", "Quiz $quizId processed: $isProcessed")
        return isProcessed
    }
    
    fun getProcessedQuizzes(): Set<String> {
        val gson = Gson()
        val json = sharedPreferences.getString(PROCESSED_QUIZZES_KEY, null)
        val type = object : TypeToken<List<String>>() {}.type
        val list = gson.fromJson<List<String>>(json, type) ?: emptyList()
        return list.toSet()
    }
    
    private fun saveProcessedQuizzes(quizzes: List<String>) {
        val gson = Gson()
        val json = gson.toJson(quizzes)
        sharedPreferences.edit {
            putString(PROCESSED_QUIZZES_KEY, json)
        }
    }
    
    fun clearProcessedQuizzes() {
        sharedPreferences.edit {
            remove(PROCESSED_QUIZZES_KEY)
        }
        Log.d("ProcessedOverdueQuizzes", "Cleared all processed quizzes")
    }
    
    companion object {
        private const val PREFS_NAME = "com.veigar.questtracker.ProcessedOverdueQuizzesPrefs"
        private const val PROCESSED_QUIZZES_KEY = "processed_overdue_quizzes"
    }
}

