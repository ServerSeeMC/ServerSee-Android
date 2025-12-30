package cn.lemwood.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun AdvancedColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit
) {
    var hsv by remember {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
        mutableStateOf(Triple(hsv[0], hsv[1], hsv[2]))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Saturation/Value Square
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(hsv.first) {
                    detectDragGestures { change, _ ->
                        val s = (change.position.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        hsv = hsv.copy(second = s, third = v)
                        onColorChanged(Color.hsv(hsv.first, hsv.second, hsv.third))
                    }
                }
                .pointerInput(hsv.first) {
                    detectTapGestures { offset ->
                        val s = (offset.x / size.width).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        hsv = hsv.copy(second = s, third = v)
                        onColorChanged(Color.hsv(hsv.first, hsv.second, hsv.third))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val saturationGradient = Brush.horizontalGradient(
                    colors = listOf(Color.White, Color.hsv(hsv.first, 1f, 1f))
                )
                val valueGradient = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
                drawRect(brush = saturationGradient)
                drawRect(brush = valueGradient)

                // Selector
                val x = hsv.second * size.width
                val y = (1f - hsv.third) * size.height
                drawCircle(
                    color = if (hsv.third > 0.5f) Color.Black else Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Hue Slider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val h = (change.position.x / size.width).coerceIn(0f, 360f)
                        hsv = hsv.copy(first = h)
                        onColorChanged(Color.hsv(hsv.first, hsv.second, hsv.third))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val h = (offset.x / size.width).coerceIn(0f, 360f)
                        hsv = hsv.copy(first = h)
                        onColorChanged(Color.hsv(hsv.first, hsv.second, hsv.third))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hues = (0..360).map { Color.hsv(it.toFloat(), 1f, 1f) }
                drawRect(brush = Brush.horizontalGradient(hues))

                // Selector
                val x = (hsv.first / 360f) * size.width
                drawRect(
                    color = Color.White,
                    topLeft = Offset(x - 2.dp.toPx(), 0f),
                    size = androidx.compose.ui.geometry.Size(4.dp.toPx(), size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Result Preview
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hsv.first, hsv.second, hsv.third))
            )
            Text(
                text = String.format("#%06X", (0xFFFFFF and Color.hsv(hsv.first, hsv.second, hsv.third).toArgb())),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
