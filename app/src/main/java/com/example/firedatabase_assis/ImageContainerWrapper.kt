package com.example.firedatabase_assis

import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton


class ImageContainerWrapper(
    var containerTagsMap: MutableMap<String, ContainerTags>,
    var priority: Int = 0,
    var imageView: ImageView? = null,
    var btnLike: ToggleButton? = null,
    var btnDislike: ToggleButton? = null,
    var btnSaved: ToggleButton? = null,
    var captionTextView: TextView? = null,
    var containerLayoutId: String? = null,
    var imageUrl: String? = null,
) : Comparable<ImageContainerWrapper> {

    fun updatePriority() {
        // Set higher priority for containers with specific serviceTag and genreTag
        priority =
            if (containerTagsMap.get(containerLayoutId)?.service == "prime" || containerTagsMap.get(
                    containerLayoutId
                )?.genre?.contains("comedy") == true
            ) {
                1 // Set a higher priority value
            } else {
                0 // Default priority
            }
    }

    // Function to compare based on priority
    override fun compareTo(other: ImageContainerWrapper): Int {
        return this.priority.compareTo(other.priority)
    }
}