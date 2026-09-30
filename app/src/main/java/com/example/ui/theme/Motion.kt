package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/**
 * Unified Motion System Tokens for Goony.
 * Defines standard durations, easing curves, and spatial fractions for consistent motion design.
 */
object MotionTokens {
    // Standard Durations (ms)
    const val DurationExtraShort = 140
    const val DurationShort = 180
    const val DurationMedium = 240
    const val DurationLong = 280
    const val DurationExtraLong = 320

    // Standard Easing Curves
    val EasingStandard: Easing = FastOutSlowInEasing
    val EasingEmphasizedDecelerate: Easing = LinearOutSlowInEasing
    val EasingEmphasizedAccelerate: Easing = FastOutLinearInEasing
    val EasingPredictiveBack: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

    // Spatial Fractions (Divisors of container dimensions)
    const val SlideFractionHorizontal = 10
    const val SlideFractionVertical = 8
    const val SlideFractionSubtle = 16
    const val SlideFractionSubtleVertical = 20

    // Scale Factors
    const val ScaleOverlayInitial = 0.96f
    const val ScalePredictiveBackMin = 0.94f
}
