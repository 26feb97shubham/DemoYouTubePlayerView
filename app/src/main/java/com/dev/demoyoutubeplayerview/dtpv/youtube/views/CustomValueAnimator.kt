package com.dev.demoyoutubeplayerview.dtpv.youtube.views

import android.animation.Animator
import android.animation.ValueAnimator
import androidx.core.util.Consumer
import com.dev.demoyoutubeplayerview.dtpv.youtube.views.SecondsView.Companion.cycleDuration

internal class CustomValueAnimator(start: Runnable, update: Consumer<Float?>, end: Runnable) :
    ValueAnimator() {
    init {
        duration = getCycleDuration() / 5.toLong()
        setFloatValues(0f, 1f)
        addUpdateListener { animation -> update.accept(animation.animatedValue as Float) }
        addListener(object : AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                start.run()
            }

            override fun onAnimationEnd(animation: Animator) {
                end.run()
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
    }

    fun getCycleDuration(): Long {
        return cycleDuration
    }
}