package com.ngocdevv.xheart

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.roundToInt

/** Native port of src/components/x-heart-animation, using Android Canvas and ValueAnimator. */
class XHeartAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    initialLiked: Boolean = false,
    var onToggle: ((Boolean) -> Unit)? = null,
    debugMode: Boolean = false,
) : View(context, attrs) {
    private var liked = initialLiked
    val isLiked: Boolean get() = liked

    var debugMode: Boolean = debugMode
        set(value) {
            field = value
            isEnabled = !value
            isClickable = !value
        }

    private var progress = if (initialLiked) 1f else 0f
    private var animator: ValueAnimator? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPath = Path().apply { fillType = Path.FillType.EVEN_ODD }
    private val heartPath = createHeartPath()

    init {
        isFocusable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        // No ripple, press scaling, or default button background in the RN source.
        background = null
        setOnClickListener { setLiked(!liked, animate = true, notify = true) }
        isClickable = !debugMode
        isEnabled = !debugMode
        updateAccessibility()
    }

    /** Change state externally; notify is opt-in so binding updates do not loop. */
    fun setLiked(liked: Boolean, animate: Boolean = true, notify: Boolean = false) {
        if (this.liked == liked) {
            if (!animate) {
                animator?.cancel()
                animator = null
                progress = if (liked) 1f else 0f
                invalidate()
            }
            return
        }
        animator?.cancel()
        animator = null
        this.liked = liked
        updateAccessibility()

        if (animate) {
            if (liked) performMediumImpact()
            // A rapid toggle starts at the current presentation progress, without jumping.
            animator = ValueAnimator.ofFloat(progress, if (liked) 1f else 0f).apply {
                duration = if (liked) HeartAnimationMath.LIKE_DURATION_MS else HeartAnimationMath.UNLIKE_DURATION_MS
                interpolator = if (liked) HeartAnimationMath.likeEasing else HeartAnimationMath.unlikeEasing
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            progress = if (liked) 1f else 0f
            invalidate()
        }

        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        if (notify) {
            try {
                onToggle?.invoke(liked)
            } catch (error: Exception) {
                Log.e("XHeartAnimation", "Error in onToggle callback", error)
            }
        }
    }

    override fun performClick(): Boolean = if (debugMode) false else super.performClick()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (HeartAnimationMath.CANVAS_SIZE * resources.displayMetrics.density).roundToInt()
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val drawing = canvas.save()
        canvas.translate(
            (width - HeartAnimationMath.CANVAS_SIZE * density) / 2f,
            (height - HeartAnimationMath.CANVAS_SIZE * density) / 2f,
        )
        canvas.scale(density, density)
        // Source Canvas clips its 58 x 58 viewport.
        canvas.clipRect(0f, 0f, HeartAnimationMath.CANVAS_SIZE, HeartAnimationMath.CANVAS_SIZE)

        if (liked) {
            drawRing(canvas)
            drawParticles(canvas)
        }
        drawHeart(canvas)
        canvas.restoreToCount(drawing)
    }

    private fun drawRing(canvas: Canvas) {
        val scale = HeartAnimationMath.ringScale(progress)
        val opacity = HeartAnimationMath.ringOpacity(progress)
        if (scale <= 0f || opacity <= 0f) return
        val inner = HeartAnimationMath.ringInnerRadius(progress)
        ringPath.rewind()
        ringPath.fillType = Path.FillType.EVEN_ODD
        ringPath.addCircle(HeartAnimationMath.CENTER, HeartAnimationMath.CENTER, 16f, Path.Direction.CW)
        if (inner > 0f) {
            ringPath.addCircle(HeartAnimationMath.CENTER, HeartAnimationMath.CENTER, inner, Path.Direction.CW)
        }
        paint.style = Paint.Style.FILL
        paint.color = HeartAnimationMath.ringColor(progress)
        paint.alpha = (opacity.coerceIn(0f, 1f) * 255).roundToInt()
        val layer = canvas.save()
        canvas.scale(scale, scale, HeartAnimationMath.CENTER, HeartAnimationMath.CENTER)
        canvas.drawPath(ringPath, paint)
        canvas.restoreToCount(layer)
    }

    private fun drawParticles(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        for (particle in HeartAnimationMath.particles) {
            val adjusted = HeartAnimationMath.adjustedParticleProgress(progress, particle.pairIndex)
            val size = HeartAnimationMath.particleSize(adjusted)
            val opacity = HeartAnimationMath.particleOpacity(adjusted)
            if (size <= 0f || opacity <= 0f) continue
            val scale = HeartAnimationMath.particleRadius(adjusted) / HeartAnimationMath.GROUP_RADIUS
            paint.color = HeartAnimationMath.particleColor(progress, particle)
            paint.alpha = (opacity.coerceIn(0f, 1f) * 255).roundToInt()
            canvas.drawCircle(
                HeartAnimationMath.CENTER + particle.x * scale,
                HeartAnimationMath.CENTER + particle.y * scale,
                size,
                paint,
            )
        }
    }

    private fun drawHeart(canvas: Canvas) {
        paint.color = HeartAnimationMath.heartColor(progress)
        paint.alpha = 255
        paint.style = if (HeartAnimationMath.isHeartOutline(progress)) Paint.Style.STROKE else Paint.Style.FILL
        paint.strokeWidth = 2f
        val layer = canvas.save()
        val scale = HeartAnimationMath.heartScale(progress)
        canvas.scale(scale, scale, HeartAnimationMath.CENTER, HeartAnimationMath.CENTER)
        canvas.translate(HeartAnimationMath.CENTER, HeartAnimationMath.CENTER)
        val svgScale = HeartAnimationMath.HEART_SIZE / 24f
        canvas.scale(svgScale, svgScale)
        canvas.translate(-12f, -12f)
        canvas.drawPath(heartPath, paint)
        canvas.restoreToCount(layer)
    }

    private fun updateAccessibility() {
        contentDescription = context.getString(if (liked) R.string.heart_action_unlike else R.string.heart_action_like)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            stateDescription = context.getString(if (liked) R.string.heart_state_liked else R.string.heart_state_unliked)
        }
    }

    @Suppress("DEPRECATION") // Legacy checked state remains supported on minSdk23 through Android16.
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Button"
        info.isCheckable = true
        info.isChecked = liked
        info.isClickable = !debugMode
    }

    private fun performMediumImpact() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        // expo-haptics SDK 54 ImpactFeedbackStyle.Medium uses exactly this waveform.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 43), intArrayOf(0, 50), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 43), -1)
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        progress = if (liked) 1f else 0f
        super.onDetachedFromWindow()
    }

    companion object {
        /** The same relative cubic commands as HEART_SVG; no approximation or stock icon. */
        private fun createHeartPath(): Path = Path().apply {
            moveTo(20.884f, 13.19f)
            rCubicTo(-1.351f, 2.48f, -4.001f, 5.12f, -8.379f, 7.67f)
            rLineTo(-0.503f, 0.3f)
            rLineTo(-0.504f, -0.3f)
            rCubicTo(-4.379f, -2.55f, -7.029f, -5.19f, -8.382f, -7.67f)
            rCubicTo(-1.36f, -2.5f, -1.41f, -4.86f, -0.514f, -6.67f)
            rCubicTo(0.887f, -1.79f, 2.647f, -2.91f, 4.601f, -3.01f)
            rCubicTo(1.651f, -0.09f, 3.368f, 0.56f, 4.798f, 2.01f)
            rCubicTo(1.429f, -1.45f, 3.146f, -2.1f, 4.796f, -2.01f)
            rCubicTo(1.954f, 0.1f, 3.714f, 1.22f, 4.601f, 3.01f)
            rCubicTo(0.896f, 1.81f, 0.846f, 4.17f, -0.514f, 6.67f)
            close()
        }
    }
}
