package com.ngocdevv.xheart

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import kotlin.math.roundToInt

class MainActivity : Activity() {
    private lateinit var heart: XHeartAnimationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        heart = XHeartAnimationView(
            context = this,
            initialLiked = savedInstanceState?.getBoolean(STATE_LIKED) ?: false,
            onToggle = { liked -> android.util.Log.d("XHeartAnimation", "Liked: $liked") },
        )
        val size = (HeartAnimationMath.CANVAS_SIZE * resources.displayMetrics.density).roundToInt()
        root.addView(heart, FrameLayout.LayoutParams(size, size, Gravity.CENTER))

        // App.tsx uses a full flex root with edgeToEdgeEnabled and no SafeAreaView.
        // Match its full-viewport center; only the system chrome occupies the edges.
        setContentView(root)
        // PhoneWindow's insets controller needs the decor created by setContentView.
        configureSystemBars()
    }

    @Suppress("DEPRECATION")
    private fun configureSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.setSystemBarsAppearance(0, android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_LIKED, heart.isLiked)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_LIKED = "liked"
    }
}
