package com.chiniyar.app

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

/** Local compatibility extension used by the offline metro Canvas. */
fun Color.toArgb(): Int = AndroidColor.argb(
    (alpha * 255f).roundToInt().coerceIn(0, 255),
    (red * 255f).roundToInt().coerceIn(0, 255),
    (green * 255f).roundToInt().coerceIn(0, 255),
    (blue * 255f).roundToInt().coerceIn(0, 255)
)
