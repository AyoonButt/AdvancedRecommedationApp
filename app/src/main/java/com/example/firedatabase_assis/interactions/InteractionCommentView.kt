package com.example.firedatabase_assis.interactions


import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.databinding.CommentInteractionItemBinding
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

class InteractionCommentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val binding: CommentInteractionItemBinding = CommentInteractionItemBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private val viewScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var onCommentClicked: ((CommentDto) -> Unit)? = null
    private var currentComment: CommentDto? = null

    private val api: Comments by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Comments::class.java)
    }

    init {
        binding.root.setOnClickListener {
            currentComment?.let { comment ->
                onCommentClicked?.invoke(comment)
            }
        }
    }

    fun setComment(comment: CommentDto, onClick: (CommentDto) -> Unit) {
        currentComment = comment
        onCommentClicked = onClick

        binding.apply {
            commentAuthor.text = comment.username
            commentContent.text = comment.content

            repliedToUser.visibility =
                if (comment.parentCommentId != null) View.VISIBLE else View.GONE
            comment.parentCommentId?.let { parentId -> loadParentUsername(parentId) }
        }
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