package com.spotifuck.music

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView

class LockableHScrollView(context: Context, attributeSet: AttributeSet?) : HorizontalScrollView(context, attributeSet) {

    var isScrollingEnabled = true
        set(value) {
            field = value
            if (!value) {
                super.scrollTo(0, 0)
            }
        }

    init {
        isSmoothScrollingEnabled = true
    }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        return if (!isScrollingEnabled) false else super.onInterceptTouchEvent(motionEvent)
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        return if (!isScrollingEnabled) false else super.onTouchEvent(motionEvent)
    }

    override fun scrollTo(x: Int, y: Int) {
        if (isScrollingEnabled) {
            super.scrollTo(x, y)
        }
    }

    override fun scrollBy(x: Int, y: Int) {
        if (isScrollingEnabled) {
            super.scrollBy(x, y)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}
