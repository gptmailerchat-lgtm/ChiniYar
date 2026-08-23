package com.chiniyar.app

import android.graphics.Canvas as AndroidCanvas
import androidx.compose.ui.graphics.Canvas

/**
 * Compatibility bridge for Compose versions where the nativeCanvas extension
 * is not imported/available from the current source set.
 */
val Canvas.nativeCanvas: AndroidCanvas
    get() {
        val canvasClass = this.javaClass
        var current: Class<*>? = canvasClass
        while (current != null) {
            for (field in current.declaredFields) {
                if (AndroidCanvas::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    return field.get(this) as AndroidCanvas
                }
            }
            current = current.superclass
        }
        throw IllegalStateException("Unable to access the underlying Android Canvas")
    }
