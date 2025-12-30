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

    // ANSI Escape Code Mapping (Standard 8/16 colors)
    private val ansiColorMap = mapOf(
        30 to Color(0xFF000000), // Black
        31 to Color(0xFFAA0000), // Red
        32 to Color(0xFF00AA00), // Green
        33 to Color(0xFFAA5500), // Yellow/Brown
        34 to Color(0xFF0000AA), // Blue
        35 to Color(0xFFAA00AA), // Magenta
        36 to Color(0xFF00AAAA), // Cyan
        37 to Color(0xFFAAAAAA), // White
        90 to Color(0xFF555555), // Bright Black (Gray)
        91 to Color(0xFFFF5555), // Bright Red
        92 to Color(0xFF55FF55), // Bright Green
        93 to Color(0xFFFFFF55), // Bright Yellow
        94 to Color(0xFF5555FF), // Bright Blue
        95 to Color(0xFFFF55FF), // Bright Magenta
        96 to Color(0xFF55FFFF), // Bright Cyan
        97 to Color(0xFFFFFFFF)  // Bright White
    )

    fun parse(text: String): AnnotatedString {
        if (text.contains("\u001B[")) {
            return parseAnsi(text)
        }
        return parseMinecraft(text)
    }

    private fun parseAnsi(text: String): AnnotatedString {
        return buildAnnotatedString {
            val ansiRegex = Regex("\u001B\\[([0-9;]*)m")
            var lastIndex = 0
            var currentColor = Color.Unspecified
            var isBold = false
            var isItalic = false
            var isUnderline = false

            ansiRegex.findAll(text).forEach { result ->
                // Append text before the escape code
                val preText = text.substring(lastIndex, result.range.first)
                if (preText.isNotEmpty()) {
                    pushStyle(SpanStyle(
                        color = currentColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                        textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None
                    ))
                    append(preText)
                    pop()
                }

                // Process ANSI codes
                val codes = result.groupValues[1].split(';').mapNotNull { it.toIntOrNull() }
                if (codes.isEmpty() || codes.contains(0)) {
                    currentColor = Color.Unspecified
                    isBold = false
                    isItalic = false
                    isUnderline = false
                } else {
                    codes.forEach { code ->
                        when {
                            code == 1 -> isBold = true
                            code == 3 -> isItalic = true
                            code == 4 -> isUnderline = true
                            code in 30..37 -> currentColor = ansiColorMap[code] ?: currentColor
                            code in 90..97 -> currentColor = ansiColorMap[code] ?: currentColor
                            code == 39 -> currentColor = Color.Unspecified // Default foreground
                        }
                    }
                }
                lastIndex = result.range.last + 1
            }

            // Append remaining text
            val remainingText = text.substring(lastIndex)
            if (remainingText.isNotEmpty()) {
                pushStyle(SpanStyle(
                    color = currentColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None
                ))
                append(remainingText)
                pop()
            }
        }
    }

    private fun parseMinecraft(text: String): AnnotatedString {
        return buildAnnotatedString {
            var currentColor = Color.Unspecified
            var isBold = false
            var isStrikethrough = false
            var isUnderlined = false
            var isItalic = false

            // 支持 § 和 & 颜色代码
            val sanitizedText = text.replace('&', '§')
            val parts = sanitizedText.split('§')
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
