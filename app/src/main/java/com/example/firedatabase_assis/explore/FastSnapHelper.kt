package com.example.firedatabase_assis.explore

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class FastSnapHelper(private val context: Context) : LinearSnapHelper() {

    private var scrollEnabled = true

    override fun createSnapScroller(layoutManager: RecyclerView.LayoutManager): LinearSmoothScroller? {
        return if (layoutManager is RecyclerView.SmoothScroller.ScrollVectorProvider) {
            object : LinearSmoothScroller(context) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return 50f / displayMetrics.densityDpi // Adjust this value to make snapping faster
                }

                override fun getVerticalSnapPreference(): Int {
                    return SNAP_TO_START
                }

                override fun getHorizontalSnapPreference(): Int {
                    return SNAP_TO_START
                }
            }
        } else {
            null
        }
    }

    fun setScrollEnabled(enabled: Boolean) {
        scrollEnabled = enabled
    }

    override fun onFling(velocityX: Int, velocityY: Int): Boolean {
        return if (scrollEnabled) super.onFling(velocityX, velocityY) else false
    }
}
