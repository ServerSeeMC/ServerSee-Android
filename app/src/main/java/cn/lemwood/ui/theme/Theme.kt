package cn.lemwood.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun ServerSeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    colorSchemeType: String = "system",
    seedColorHex: String = "#6750A4",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val colorScheme = when {
        colorSchemeType == "system" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val seedColor = try {
                Color(android.graphics.Color.parseColor(seedColorHex))
            } catch (e: Exception) {
                if (darkTheme) Purple80 else Purple40
            }
            
            if (darkTheme) {
                darkColorScheme(
                    primary = seedColor,
                    secondary = seedColor.copy(alpha = 0.8f),
                    tertiary = seedColor.copy(alpha = 0.6f),
                    surface = Color(0xFF1C1B1F),
                    onSurface = Color(0xFFE6E1E5)
                )
            } else {
                lightColorScheme(
                    primary = seedColor,
                    secondary = seedColor.copy(alpha = 0.8f),
                    tertiary = seedColor.copy(alpha = 0.6f),
                    surface = Color(0xFFFFFBFE),
                    onSurface = Color(0xFF1C1B1F)
                )
            }
        }
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
