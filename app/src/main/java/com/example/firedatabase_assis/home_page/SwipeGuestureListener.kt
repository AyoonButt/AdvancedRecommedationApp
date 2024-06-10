package com.example.firedatabase_assis.home_page

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.R

class SwipeGestureListener(
    private val context: Context,
    private val onSwipeDown: () -> Unit
) : GestureDetector.SimpleOnGestureListener() {

    private val gestureDetector = GestureDetector(context, this)

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
    ): Boolean {
        // Check if e1 is null or fragment is not attached
        if (e1 == null || !isValidFragmentState()) {
            return false
        }

        // Detect swipe down gesture
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
        // Check if the fragment is attached to an activity and is in a valid state
        return (context is AppCompatActivity && context.supportFragmentManager.findFragmentById(
            R.id.fragment_container
        ) != null)
    }
}
