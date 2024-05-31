package com.example.firedatabase_assis.home_page

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class SwipeGestureListener(
    context: Context,
    private val onSwipeDown: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector = GestureDetector(context, this)

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        // Detect swipe down gesture
        if (e1 != null && e2 != null && e2.y > e1.y) {
            onSwipeDown()
            return true
        }
        return false
    }

    fun onTouch(view: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}



