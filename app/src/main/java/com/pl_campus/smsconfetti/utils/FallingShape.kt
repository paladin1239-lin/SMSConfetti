package com.pl_campus.smsconfetti.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.ui.graphics.Color

data class FallingShape(
    val id: Int,
    val color: Color,
    val initialXOffsetPx: Float, // Horizontal position (fixed for simplicity)
    val animatableY: Animatable<Float, AnimationVector1D>,
    var hasStartedFalling: Boolean = false,
    var shapeState: ShapeState = ShapeState.RECTANGLE
)