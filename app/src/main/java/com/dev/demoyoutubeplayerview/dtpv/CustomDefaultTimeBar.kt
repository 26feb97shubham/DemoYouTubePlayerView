package com.dev.demoyoutubeplayerview.dtpv

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.exoplayer2.ui.DefaultTimeBar
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationTargetException

class CustomDefaultTimeBar @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    timebarAttrs: AttributeSet? = attrs,
    defStyleRes: Int = 0
) :
    DefaultTimeBar(context!!, attrs, defStyleAttr, timebarAttrs, defStyleRes) {
    var scrubberBar: Rect? = null
    private var scrubbing = false
    private var scrubbingStartX = 0
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && scrubberBar != null) {
            scrubbing = false
            scrubbingStartX = event.x.toInt()
            val distanceFromScrubber = Math.abs(scrubberBar!!.right - scrubbingStartX)
            scrubbing = if (distanceFromScrubber > Utils.dpToPx(24)) return true else true
        }
        if (!scrubbing && event.action == MotionEvent.ACTION_MOVE && scrubberBar != null) {
            val distanceFromStart = Math.abs(event.x.toInt() - scrubbingStartX)
            if (distanceFromStart > Utils.dpToPx(6)) {
                scrubbing = true
                try {
                    val method =
                        DefaultTimeBar::class.java.getDeclaredMethod(
                            "startScrubbing",
                            Long::class.javaPrimitiveType
                        )
                    method.isAccessible = true
                    method.invoke(this, 0.toLong())
                } catch (e: NoSuchMethodException) {
                    e.printStackTrace()
                } catch (e: SecurityException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: InvocationTargetException) {
                    e.printStackTrace()
                }
            } else {
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    init {
        try {
            val field = DefaultTimeBar::class.java.getDeclaredField("scrubberBar")
            field.isAccessible = true
            scrubberBar = field[this] as Rect
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }
}
