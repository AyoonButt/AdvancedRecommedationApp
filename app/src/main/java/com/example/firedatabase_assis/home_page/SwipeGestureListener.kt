package com.example.firedatabase_assis.home_page

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.R
import kotlin.math.abs

class SwipeGestureListener(
    private val context: Context,
    private var fragmentContainer: View,
    private val onSwipeDown: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector = GestureDetector(context, this)
    private var initialY: Int = 0
    private var initialMarginTop: Int = 0
    private var isSwipeInProgress = false

    // Threshold for vertical movement to be considered a swipe
    private val SWIPE_THRESHOLD = 50
    private val SWIPE_VELOCITY_THRESHOLD = 100


    override fun onDown(e: MotionEvent): Boolean {
        initialY = e.rawY.toInt()
        val layoutParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
        initialMarginTop = layoutParams.topMargin
        return false  // Return false to allow other touch events to be processed
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (e1 == null) return false

        val deltaY = e2.rawY - e1.rawY

        // Only handle vertical scrolls that are primarily vertical
        if (abs(deltaY) > abs(distanceX) && deltaY > SWIPE_THRESHOLD) {
            isSwipeInProgress = true
            adjustTopMargin((e2.rawY - initialY).toInt())
            return true
        }

        return false
    }

    private fun adjustTopMargin(dy: Int) {
        val layoutParams = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
        val newMarginTop = initialMarginTop + dy
        layoutParams.topMargin = maxOf(0, newMarginTop)
        fragmentContainer.layoutParams = layoutParams
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null || !isValidFragmentState()) return false

        val deltaY = e2.y - e1.y
        val deltaX = e2.x - e1.x

        if (abs(deltaY) > SWIPE_THRESHOLD &&
            abs(velocityY) > SWIPE_VELOCITY_THRESHOLD &&
            abs(deltaY) > abs(deltaX) &&
            deltaY > 0
        ) {
            onSwipeDown()
            return true
        }
        return false
    }

    fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                if (!isSwipeInProgress) {
                    view.performClick()
                }
                handleActionUp()
            }

            MotionEvent.ACTION_CANCEL -> {
                handleActionUp()
            }
        }

        // Only consume the event if we're in the middle of a swipe
        return if (isSwipeInProgress) {
            gestureDetector.onTouchEvent(event)
        } else {
            gestureDetector.onTouchEvent(event)
            false  // Allow the event to propagate to other views
        }
    }

    private fun handleActionUp() {
        isSwipeInProgress = false
        // Reset margin if swipe wasn't completed
        if (fragmentContainer.layoutParams is ViewGroup.MarginLayoutParams) {
            val params = fragmentContainer.layoutParams as ViewGroup.MarginLayoutParams
            if (params.topMargin in 1 until SWIPE_THRESHOLD) {
                params.topMargin = 0
                fragmentContainer.layoutParams = params
            }
        }
    }

    private fun isValidFragmentState(): Boolean {
        return (context is AppCompatActivity &&
                context.supportFragmentManager.findFragmentById(R.id.fragment_container) != null)
    }


}