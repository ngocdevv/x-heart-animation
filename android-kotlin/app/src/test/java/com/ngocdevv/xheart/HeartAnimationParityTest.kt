package com.ngocdevv.xheart

import org.junit.Assert.assertEquals
import org.junit.Test

/** Independent numeric snapshots captured from the original RN derived values, not rendered pixels. */
class HeartAnimationParityTest {
    @Test
    fun sourceProgressSnapshotsMatch() {
        // p, heart scale, ring scale/inner/opacity, then both particle-pair radius/size/opacity.
        val snapshots = arrayOf(
            floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            floatArrayOf(0.03f, 0.046f, 0.1f, 0.0f, 0.24f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            floatArrayOf(0.2f, 0.30666666666666664f, 0.6666666666666667f, 0.0f, 0.8500000000000001f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
            floatArrayOf(0.25f, 0.3833333333333333f, 0.8333333333333334f, 0.0f, 0.875f, 20.0f, 1.2f, 1.0f, 0.0f, 0.0f, 0.0f),
            floatArrayOf(0.3f, 0.4599999999999999f, 1.0f, 0.0f, 0.9f, 20.333333333333332f, 1.2285714285714284f, 1.0f, 7.999999999999988f, 0.4799999999999992f, 0.39999999999999936f),
            floatArrayOf(0.5f, 0.7666666666666666f, 1.0f, 5.0f, 0.7f, 21.666666666666668f, 1.3428571428571427f, 1.0f, 21.133333333333333f, 1.2971428571428572f, 1.0f),
            floatArrayOf(0.6f, 0.9199999999999998f, 1.0f, 8.5f, 0.5666666666666667f, 22.333333333333332f, 1.4f, 1.0f, 21.8f, 1.3542857142857143f, 1.0f),
            floatArrayOf(0.75f, 1.15f, 1.0f, 12.666666666666668f, 0.3666666666666667f, 23.333333333333332f, 1.04f, 0.625f, 22.8f, 1.2319999999999998f, 0.8249999999999998f),
            floatArrayOf(0.85f, 1.05f, 1.0f, 14.0f, 0.2250000000000001f, 24.0f, 0.8f, 0.375f, 23.46666666666667f, 0.9919999999999999f, 0.575f),
            floatArrayOf(0.9f, 1.0f, 1.0f, 14.666666666666668f, 0.15f, 24.333333333333336f, 0.5333333333333332f, 0.2499999999999999f, 23.8f, 0.8719999999999998f, 0.44999999999999984f),
            floatArrayOf(0.92f, 1.03f, 1.0f, 14.933333333333334f, 0.11999999999999997f, 24.46666666666667f, 0.4266666666666664f, 0.19999999999999984f, 23.933333333333334f, 0.8239999999999998f, 0.3999999999999998f),
            floatArrayOf(1.0f, 1.15f, 1.0f, 16.0f, 0.0f, 25.0f, 0.0f, 0.0f, 24.46666666666667f, 0.4266666666666664f, 0.19999999999999984f),
        )
        for (sample in snapshots) {
            val p = sample[0]
            near(sample[1], HeartAnimationMath.heartScale(p))
            near(sample[2], HeartAnimationMath.ringScale(p))
            near(sample[3], HeartAnimationMath.ringInnerRadius(p))
            near(sample[4], HeartAnimationMath.ringOpacity(p))
            for (pair in 0..1) {
                val adjusted = HeartAnimationMath.adjustedParticleProgress(p, pair)
                near(sample[5 + pair * 3], HeartAnimationMath.particleRadius(adjusted))
                near(sample[6 + pair * 3], HeartAnimationMath.particleSize(adjusted))
                near(sample[7 + pair * 3], HeartAnimationMath.particleOpacity(adjusted))
            }
        }
        assertEquals(true, HeartAnimationMath.isHeartOutline(0f))
        assertEquals(true, HeartAnimationMath.isHeartOutline(0.029f))
        assertEquals(false, HeartAnimationMath.isHeartOutline(0.03f))
        // These two source quirks are intentional: final scale1.15 and visible second-pair residuals.
        near(1.15f, HeartAnimationMath.heartScale(1f))
        near(0.42666667f, HeartAnimationMath.particleSize(0.92f))
        near(0.2f, HeartAnimationMath.particleOpacity(0.92f))
    }

    @Test
    fun timingAndRgbSnapshotsMatch() {
        val timing = arrayOf(
            floatArrayOf(0.0f, 0.0f),
            floatArrayOf(0.04f, 0.11548072915645297f),
            floatArrayOf(0.08f, 0.22739442411071298f),
            floatArrayOf(0.12f, 0.3326013626831712f),
            floatArrayOf(0.2f, 0.514250289932317f),
            floatArrayOf(0.3f, 0.6830690140196152f),
            floatArrayOf(0.5f, 0.8772474490074499f),
            floatArrayOf(0.75f, 0.9764163025170037f),
            floatArrayOf(1.0f, 1.0f),
        )
        val colors = arrayOf(
            intArrayOf(0xffaab8c2.toInt(), 0xffdd4789.toInt()),
            intArrayOf(0xffc66f87.toInt(), 0xffdd4789.toInt()),
            intArrayOf(0xffe2264d.toInt(), 0xffdc4c90.toInt()),
            intArrayOf(0xffe2264d.toInt(), 0xffd75ead.toInt()),
            intArrayOf(0xffe2264d.toInt(), 0xffcf7fde.toInt()),
            intArrayOf(0xffe2264d.toInt(), 0xffcc8ff5.toInt()),
            intArrayOf(0xffe2264d.toInt(), 0xffcc8ff5.toInt()),
            intArrayOf(0xffe2264d.toInt(), 0xffcc8ff5.toInt()),
            intArrayOf(0xffe2264d.toInt(), 0xffcc8ff5.toInt()),
        )
        for ((index, sample) in timing.withIndex()) {
            val p = HeartAnimationMath.likeEasing.getInterpolation(sample[0])
            assertEquals(sample[1], p, 0.000001f)
            assertEquals(colors[index][0], HeartAnimationMath.heartColor(p))
            assertEquals(colors[index][1], HeartAnimationMath.ringColor(p))
        }
        assertEquals(1000L, HeartAnimationMath.LIKE_DURATION_MS)
        assertEquals(200L, HeartAnimationMath.UNLIKE_DURATION_MS)
        assertEquals(0f, HeartAnimationMath.unlikeEasing.getInterpolation(0f), 0f)
        assertEquals(0.3153568f, HeartAnimationMath.unlikeEasing.getInterpolation(0.5f), 0.000001f)
        assertEquals(1f, HeartAnimationMath.unlikeEasing.getInterpolation(1f), 0f)
    }

    @Test
    fun allFourteenParticleOriginsMatch() {
        val origins = arrayOf(
            floatArrayOf(1.28557521937308f, -21.532088886237958f),
            floatArrayOf(-1.2855752193730774f, -18.467911113762042f),
            floatArrayOf(17.63600801336223f, -12.419934653698524f),
            floatArrayOf(13.637251285358964f, -12.519657420650814f),
            floatArrayOf(20.706167064287428f, 6.044683693572132f),
            floatArrayOf(18.29094942298552f, 2.856153664680443f),
            floatArrayOf(8.184159986970576f, 19.95753193050654f),
            floatArrayOf(9.17118957773175f, 16.081222785590228f),
            floatArrayOf(-10.500686486974507f, 18.841951564309607f),
            floatArrayOf(-6.854663077727815f, 17.19680315178716f),
            floatArrayOf(-21.27830186125941f, 3.5379973644199643f),
            floatArrayOf(-17.718814626013536f, 5.3628399938326154f),
            floatArrayOf(-16.032921935759394f, -14.430141012871754f),
            floatArrayOf(-15.240337362961803f, -10.50945106147758f),
        )
        assertEquals(14, HeartAnimationMath.particles.size)
        for ((index, particle) in HeartAnimationMath.particles.withIndex()) {
            near(origins[index][0], particle.x)
            near(origins[index][1], particle.y)
            assertEquals(index % 2, particle.pairIndex)
        }
    }

    @Test
    fun particleColorClampsAtSourceEndpoints() {
        for (particle in HeartAnimationMath.particles) {
            assertEquals(particle.startColor, HeartAnimationMath.particleColor(0f, particle))
            assertEquals(particle.startColor, HeartAnimationMath.particleColor(0.20f, particle))
            assertEquals(particle.endColor, HeartAnimationMath.particleColor(1f, particle))
            assertEquals(particle.endColor, HeartAnimationMath.particleColor(2f, particle))
        }
    }

    private fun near(expected: Float, actual: Float) = assertEquals(expected, actual, 0.00002f)
}
