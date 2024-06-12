package com.example.firedatabase_assis.home_page

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.R

class SwipeGestureListener(
    private val context: Context,
    private var fragmentContainer: View,
    private val onSwipeDown: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector = GestureDetector(context, this)
    private var initialY: Int = 0
    private var initialMarginTop: Int = 0

    override fun onDown(e: MotionEvent): Boolean {
        initialY = e.rawY.toInt()
        val layoutParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
        initialMarginTop = layoutParams.topMargin
        return true
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        val currentY = e2.rawY.toInt()
        val dy = currentY - initialY
        adjustTopMargin(dy)
        return true
    }

    private fun adjustTopMargin(dy: Int) {
        val layoutParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
        val newMarginTop = initialMarginTop + dy
        layoutParams.topMargin = maxOf(0, newMarginTop) // Ensure top margin doesn't go below 0
        fragmentContainer.layoutParams = layoutParams
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null || !isValidFragmentState()) {
            return false
        }
        if (e2.y > e1.y) {
            onSwipeDown()
            return true
        }
        return false
    }

    fun onTouch(view: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private fun isValidFragmentState(): Boolean {
        return (context is AppCompatActivity && context.supportFragmentManager.findFragmentById(R.id.fragment_container) != null)
    }

    fun updateViewBounds(fragmentContainer: View) {
        this.fragmentContainer = fragmentContainer
    }
}
