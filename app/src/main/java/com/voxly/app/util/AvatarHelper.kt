package com.voxly.app.util

import android.content.Context
import com.voxly.app.R

object AvatarHelper {
    fun getMaleAvatars(): List<String> {
        val list = mutableListOf<String>()
        for (i in 1..10) {
            list.add("male_avatar_$i")
        }
        return list
    }

    fun getFemaleAvatars(): List<String> {
        val list = mutableListOf<String>()
        for (i in 1..10) {
            list.add("female_avatar_$i")
        }
        return list
    }

    fun getDrawableId(context: Context, avatarName: String): Int {
        return context.resources.getIdentifier(avatarName, "drawable", context.packageName)
    }

    suspend fun getDynamicBackgroundColor(context: Context, resId: Int): androidx.compose.ui.graphics.Color {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
                if (bitmap != null) {
                    val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
                    // Try to get a vibrant or dominant color, defaulting to a fallback
                    val colorInt = palette.getVibrantColor(
                        palette.getDominantColor(android.graphics.Color.parseColor("#3E2723"))
                    )
                     androidx.compose.ui.graphics.Color(colorInt)
                } else {
                     androidx.compose.ui.graphics.Color(0xFF3E2723) // Fallback Brown
                }
            } catch (e: Exception) {
                 androidx.compose.ui.graphics.Color(0xFF3E2723) // Fallback
            }
        }
    }

    fun getTagEmoji(tag: String): String {
        return when (tag.lowercase().trim()) {
            "music" -> "🎶"
            "cinema" -> "🎬"
            "travel" -> "✈️"
            "deep talks" -> "💬"
            "books" -> "📚"
            "gaming" -> "🎮"
            "english practice" -> "🗣️"
            "casual chat" -> "💬"
            "fitness" -> "💪"
            "cooking" -> "🍳"
            "tech" -> "💻"
            "art" -> "🎨"
            "politics" -> "🗳️"
            "science" -> "🔬"
            "history" -> "📜"
            else -> "✨"
        }
    }
}
