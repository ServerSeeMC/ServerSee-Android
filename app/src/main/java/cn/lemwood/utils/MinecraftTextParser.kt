package cn.lemwood.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object MinecraftTextParser {
    private val colorMap = mapOf(
        '0' to Color(0xFF000000), // Black
        '1' to Color(0xFF0000AA), // Dark Blue
        '2' to Color(0xFF00AA00), // Dark Green
        '3' to Color(0xFF00AAAA), // Dark Aqua
        '4' to Color(0xFFAA0000), // Dark Red
        '5' to Color(0xFFAA00AA), // Dark Purple
        '6' to Color(0xFFFFAA00), // Gold
        '7' to Color(0xFFAAAAAA), // Gray
        '8' to Color(0xFF555555), // Dark Gray
        '9' to Color(0xFF5555FF), // Blue
        'a' to Color(0xFF55FF55), // Green
        'b' to Color(0xFF55FFFF), // Aqua
        'c' to Color(0xFFFF5555), // Red
        'd' to Color(0xFFFF55FF), // Light Purple
        'e' to Color(0xFFFFFF55), // Yellow
        'f' to Color(0xFFFFFFFF)  // White
    )

    fun parse(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentColor = Color.Unspecified
            var isBold = false
            var isStrikethrough = false
            var isUnderlined = false
            var isItalic = false

            val parts = text.split('§')
            append(parts[0]) // Append text before first §

            for (i in 1 until parts.size) {
                val part = parts[i]
                if (part.isEmpty()) continue

                val code = part[0].lowercaseChar()
                val remainingText = part.substring(1)

                when (code) {
                    in '0'..'9', in 'a'..'f' -> {
                        currentColor = colorMap[code] ?: Color.Unspecified
                        // In Minecraft, a color code resets previous formatting
                        isBold = false
                        isStrikethrough = false
                        isUnderlined = false
                        isItalic = false
                    }
                    'l' -> isBold = true
                    'm' -> isStrikethrough = true
                    'n' -> isUnderlined = true
                    'o' -> isItalic = true
                    'r' -> { // Reset
                        currentColor = Color.Unspecified
                        isBold = false
                        isStrikethrough = false
                        isUnderlined = false
                        isItalic = false
                    }
                }

                pushStyle(
                    SpanStyle(
                        color = currentColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = when {
                            isStrikethrough && isUnderlined -> TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
                            isStrikethrough -> TextDecoration.LineThrough
                            isUnderlined -> TextDecoration.Underline
                            else -> TextDecoration.None
                        }
                    )
                )
                append(remainingText)
                pop()
            }
        }
    }
}
