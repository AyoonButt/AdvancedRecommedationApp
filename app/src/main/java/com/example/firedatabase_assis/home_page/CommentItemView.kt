package com.example.firedatabase_assis.home_page

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.databinding.CommentItemBinding
import com.example.firedatabase_assis.postgres.CommentDto
import com.example.firedatabase_assis.postgres.Comments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val binding: CommentItemBinding = CommentItemBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isClickEnabled = true
    private var isSelectedState = false
    private var currentComment: CommentDto? = null
    private var onSelectionChanged: ((Int, Boolean) -> Unit)? = null

    private val api: Comments by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Comments::class.java)
    }

    init {
        setOnClickListener {
            if (isClickEnabled) {
                // Existing selection logic
                if (isSelectedState) {
                    currentComment?.commentId?.let { commentId ->
                        onSelectionChanged?.invoke(commentId, false)
                    }
                }
            }
        }
    }

    override fun setSelected(selected: Boolean) {
        super.setSelected(selected)
        isSelectedState = selected
        binding.commentContainer.setBackgroundColor(
            if (isSelectedState)
                ContextCompat.getColor(context, R.color.selected_comment_background)
            else
                ContextCompat.getColor(context, android.R.color.transparent)
        )
    }

    override fun isSelected(): Boolean {
        return isSelectedState
    }

    fun setComment(
        comment: CommentDto,
        isSelected: Boolean = false,
        onSelection: ((Int, Boolean) -> Unit)? = null
    ) {
        currentComment = comment
        onSelectionChanged = onSelection

        binding.apply {
            commentAuthor.text = comment.username
            commentContent.text = comment.content
            repliedToUser.visibility =
                if (comment.parentCommentId != null) View.VISIBLE else View.GONE

            comment.parentCommentId?.let { parentId -> loadParentUsername(parentId) }
        }

        setSelected(isSelected)
    }

    private fun loadParentUsername(parentId: Int) {
        viewScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    api.getParentCommentUsername(parentId)
                }

                if (response.isSuccessful && response.body()?.success == true) {
                    binding.repliedToUser.text = "Replying to @${response.body()?.message}"
                } else {
                    binding.repliedToUser.visibility = GONE
                }
            } catch (e: Exception) {
                binding.repliedToUser.visibility = GONE
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope.cancel()
    }
}