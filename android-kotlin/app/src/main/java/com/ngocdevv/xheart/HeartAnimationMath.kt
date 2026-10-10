package com.ngocdevv.xheart

import android.animation.TimeInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** All distances are source canvas units (dp), not physical display pixels. */
internal object HeartAnimationMath {
    const val CANVAS_SIZE = 58f
    const val HEART_SIZE = 26f
    const val CENTER = CANVAS_SIZE / 2f
    const val LIKE_DURATION_MS = 1000L
    const val UNLIKE_DURATION_MS = 200L
    const val GROUP_RADIUS = 20f

    val likeEasing = CubicBezierInterpolator(0.21, 0.61, 0.35, 1.0)

    // Reanimated 4.1.1 Easing.ease = Bezier(0.42, 0, 1, 1), not CSS "ease".
    val unlikeEasing = CubicBezierInterpolator(0.42, 0.0, 1.0, 1.0)

    private val heartColorInput = floatArrayOf(0f, 0.03f, 0.20f, 0.30f, 1f)
    private val heartColorOutput = intArrayOf(
        hex("#aab8c2"), hex("#aab8c2"), hex("#e2264d"),
        hex("#e2264d"), hex("#e2264d"),
    )
    private val heartScaleInput = floatArrayOf(0f, 0.75f, 0.9f, 1f)
    // The fifth output in the original RN code has no matching input, so it is unused.
    private val heartScaleOutput = floatArrayOf(0f, 1.15f, 1f, 1.15f)
    private val ringScaleInput = floatArrayOf(0f, 0.3f, 1f)
    private val ringScaleOutput = floatArrayOf(0f, 1f, 1f)
    private val ringInnerInput = floatArrayOf(0f, 0.3f, 0.5f, 0.7f, 1f)
    private val ringInnerOutput = floatArrayOf(0f, 0f, 5f, 12f, 16f)
    private val ringOpacityInput = floatArrayOf(0f, 0.1f, 0.3f, 0.5f, 0.8f, 1f)
    private val ringOpacityOutput = floatArrayOf(0f, 0.8f, 0.9f, 0.7f, 0.3f, 0f)
    private val ringColorInput = floatArrayOf(0f, 0.2f, 0.4f, 0.6f, 1f)
    private val ringColorOutput = intArrayOf(
        hex("#DD4789"), hex("#DD4789"), hex("#D46ABF"),
        hex("#CC8FF5"), hex("#CC8FF5"),
    )
    private val particleRadiusInput = floatArrayOf(0.25f, 1f)
    private val particleRadiusOutput = floatArrayOf(20f, 25f)
    private val particleSizeInput = floatArrayOf(0.25f, 0.60f, 0.85f, 1f)
    private val particleSizeOutput = floatArrayOf(1.2f, 1.4f, 0.8f, 0f)

    data class Particle(
        val x: Float,
        val y: Float,
        val startColor: Int,
        val endColor: Int,
        val pairIndex: Int,
    )

    val particles: List<Particle> = buildList {
        val colors = arrayOf(
            arrayOf("#8CE8C3", "#A068CE", "#8BE7C2", "#B752E1"),
            arrayOf("#90D2FA", "#99E9C8", "#91D1F9", "#BAE3D7"),
            arrayOf("#CC8EF5", "#D3F491", "#CB8DF4", "#DCE483"),
            arrayOf("#8CE8C3", "#59C392", "#8CE8C3", "#67CD9F"),
            arrayOf("#F58EA7", "#CAADC7", "#F48DA6", "#959FF3"),
            arrayOf("#91D2FA", "#CA5ED8", "#91D2FA", "#A975D1"),
            arrayOf("#92D3FC", "#C35DD1", "#CB8DF4", "#90E0BE"),
        )
        repeat(7) { group ->
            val groupAngle = group * 2.0 * PI / 7.0 - PI / 2.0
            val groupX = GROUP_RADIUS * cos(groupAngle)
            val groupY = GROUP_RADIUS * sin(groupAngle)
            repeat(2) { pair ->
                val angle = groupAngle + 40.0 * PI / 180.0 + pair * PI
                add(
                    Particle(
                        x = (groupX + 2.0 * cos(angle)).toFloat(),
                        y = (groupY + 2.0 * sin(angle)).toFloat(),
                        startColor = hex(colors[group][pair * 2]),
                        endColor = hex(colors[group][pair * 2 + 1]),
                        pairIndex = pair,
                    ),
                )
            }
        }
    }

    fun heartScale(progress: Float): Float =
        if (progress == 0f) 1f else interpolate(progress, heartScaleInput, heartScaleOutput)

    fun heartColor(progress: Float): Int = interpolateColor(progress, heartColorInput, heartColorOutput)
    fun isHeartOutline(progress: Float): Boolean = progress < 0.03f
    fun ringScale(progress: Float): Float = interpolate(progress, ringScaleInput, ringScaleOutput)
    fun ringInnerRadius(progress: Float): Float = interpolate(progress, ringInnerInput, ringInnerOutput)
    fun ringOpacity(progress: Float): Float = interpolate(progress, ringOpacityInput, ringOpacityOutput)
    fun ringColor(progress: Float): Int = interpolateColor(progress, ringColorInput, ringColorOutput)

    /** Stagger offsets are subtracted from eased progress, not from elapsed milliseconds. */
    fun adjustedParticleProgress(progress: Float, pairIndex: Int): Float = progress - pairIndex * 0.08f

    fun particleRadius(adjusted: Float): Float = when {
        adjusted < 0.20f -> 0f
        adjusted < 0.25f -> (adjusted - 0.20f) / 0.05f * GROUP_RADIUS
        else -> interpolate(adjusted, particleRadiusInput, particleRadiusOutput)
    }

    fun particleSize(adjusted: Float): Float = when {
        adjusted < 0.20f -> 0f
        adjusted < 0.25f -> (adjusted - 0.20f) / 0.05f * 1.2f
        else -> interpolate(adjusted, particleSizeInput, particleSizeOutput)
    }

    fun particleOpacity(adjusted: Float): Float = when {
        adjusted < 0.20f -> 0f
        adjusted < 0.25f -> (adjusted - 0.20f) / 0.05f
        adjusted < 0.60f -> 1f
        else -> 1f - (adjusted - 0.60f) / 0.40f
    }

    fun particleColor(progress: Float, particle: Particle): Int =
        mixColor(particle.startColor, particle.endColor, (progress - 0.20f) / 0.80f)

    private fun interpolate(value: Float, input: FloatArray, output: FloatArray): Float {
        val segment = input.indexOfFirst { value <= it }.let {
            when {
                it <= 0 -> if (it == -1) input.lastIndex - 1 else 0
                else -> it - 1
            }
        }
        val fraction = (value - input[segment]) / (input[segment + 1] - input[segment])
        return output[segment] + fraction * (output[segment + 1] - output[segment])
    }

    private fun interpolateColor(value: Float, input: FloatArray, output: IntArray): Int {
        val clamped = value.coerceIn(input.first(), input.last())
        val upper = input.indexOfFirst { clamped <= it }.coerceAtLeast(1)
        val fraction = (clamped - input[upper - 1]) / (input[upper] - input[upper - 1])
        return mixColor(output[upper - 1], output[upper], fraction)
    }

    // Skia interpolateColors uses raw RGB channels and clamps; no gamma correction.
    private fun mixColor(from: Int, to: Int, fraction: Float): Int {
        val t = fraction.coerceIn(0f, 1f)
        fun channel(a: Int, b: Int) = (a + (b - a) * t).roundToInt().coerceIn(0, 255)
        val red = channel(from ushr 16 and 0xff, to ushr 16 and 0xff)
        val green = channel(from ushr 8 and 0xff, to ushr 8 and 0xff)
        val blue = channel(from and 0xff, to and 0xff)
        return (0xff shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun hex(value: String): Int = (0xff shl 24) or value.removePrefix("#").toInt(16)
}

/** Invert x(t) before reading y(t); using the Bezier y polynomial directly is incorrect. */
internal class CubicBezierInterpolator(
    private val x1: Double,
    private val y1: Double,
    private val x2: Double,
    private val y2: Double,
) : TimeInterpolator {
    override fun getInterpolation(input: Float): Float {
        if (input <= 0f) return 0f
        if (input >= 1f) return 1f
        var lower = 0.0
        var upper = 1.0
        repeat(26) {
            val t = (lower + upper) / 2.0
            if (coordinate(t, x1, x2) < input.toDouble()) lower = t else upper = t
        }
        return coordinate((lower + upper) / 2.0, y1, y2).toFloat()
    }

    private fun coordinate(t: Double, first: Double, second: Double): Double {
        val inverse = 1.0 - t
        return 3.0 * inverse * inverse * t * first + 3.0 * inverse * t * t * second + t * t * t
    }
}
