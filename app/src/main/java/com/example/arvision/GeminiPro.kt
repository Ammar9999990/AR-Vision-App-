package com.example.arvision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object GeminiPro {

    private val textOnlyModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.4f
                topK = 32
                topP = 1.0f
                maxOutputTokens = 2048
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
            )
        )
    }

    private val visionModel by lazy {
        GenerativeModel(
            modelName = "gemini-pro-vision",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.4f
                topK = 32
                topP = 1.0f
                maxOutputTokens = 2048
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH),
            )
        )
    }

    suspend fun describeObject(image: Bitmap, objectLabel: String): String {
        val outputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val compressedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(outputStream.toByteArray()))

        val prompt = "Provide a brief, interesting, and informative description of this ${objectLabel}. Focus on its history, design, or common uses. Keep the response to 2-3 sentences."
        val inputContent = content {
            image(compressedBitmap)
            text(prompt)
        }

        return try {
            val response = visionModel.generateContent(inputContent)
            response.text ?: "Could not get a description at this time."
        } catch (e: Exception) {
            Log.e("GeminiPro", "Error describing object", e)
            "Error: Could not get a description. Please check your internet connection and API key, then try again."
        }
    }

    suspend fun generateActivitySummary(history: List<HistoryItem>): String {
        val historyText = history.joinToString("\n") { "- ${it.eventType}: ${it.data}" }
        val prompt = "Analyze the following user activity log and generate a short, friendly, and insightful summary of their week. Mention their most common activities and any interesting patterns. Here is the log:\n\n$historyText"

        return try {
            val response = textOnlyModel.generateContent(prompt)
            response.text ?: "Could not generate a summary at this time."
        } catch (e: Exception) {
            Log.e("GeminiPro", "Error generating summary", e)
            "Error: Could not generate a summary. Please check your internet connection and API key, then try again."
        }
    }

    suspend fun getDeeperAnalysis(objectName: String): String {
        val prompt = "Provide a brief and interesting note on the origin, history, or significance of a ${objectName}."

        return try {
            val response = textOnlyModel.generateContent(prompt)
            response.text ?: "Could not get a deeper analysis at this time."
        } catch (e: Exception) {
            Log.e("GeminiPro", "Error getting deeper analysis", e)
            "Error: Could not get a deeper analysis. Please check your internet connection and API key, then try again."
        }
    }
}
