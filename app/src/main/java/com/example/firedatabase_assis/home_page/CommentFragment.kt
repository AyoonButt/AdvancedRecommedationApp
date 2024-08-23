package com.example.firedatabase_assis.home_page

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.adapters.CommentsAdapter
import com.example.firedatabase_assis.database.Comment
import com.example.firedatabase_assis.database.Comments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

class CommentFragment(private val postId: Int) : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var swipeGestureListener: SwipeGestureListener
    private lateinit var fragmentContainerLayout: View
    private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("CommentFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_comment, container, false)
        commentsRecyclerView = view.findViewById(R.id.comment_recycler_view)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        commentsAdapter = CommentsAdapter(requireContext(), viewLifecycleOwner, listOf(), 0)
        commentsRecyclerView.adapter = commentsAdapter

        // Define the fragment container
        fragmentContainerLayout = requireActivity().findViewById(R.id.fragment_container)

        // Set up swipe gesture listener for the fragment_container layout
        swipeGestureListener = SwipeGestureListener(requireContext(), fragmentContainerLayout) {
            Log.d("SwipeGestureListener", "Swipe down action triggered")
            fragmentContainerLayout.visibility = View.GONE
            requireActivity().supportFragmentManager.popBackStack()
        }

        fragmentContainerLayout.setOnTouchListener { _, event ->
            val result = swipeGestureListener.onTouch(fragmentContainerLayout, event)
            Log.d("CommentFragment", "Touch event: $event, result: $result")
            result
        }

        commentsRecyclerView.setOnTouchListener { _, event ->
            val result = swipeGestureListener.onTouch(commentsRecyclerView, event)
            Log.d("CommentFragment", "RecyclerView touch event: $event, result: $result")
            result
        }

        // Listen for changes in layout size and update the swipe gesture listener
        fragmentContainerLayout.viewTreeObserver.addOnGlobalLayoutListener {
            swipeGestureListener.updateViewBounds(fragmentContainerLayout)
        }

        // Bring the fragment_container layout to the front
        fragmentContainerLayout.bringToFront()
        fragmentContainerLayout.requestFocus()

        // Log when the listener is attached
        Log.d("CommentFragment", "SwipeGestureListener attached to fragment_container layout")

        commentsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0
                ) {
                    loadData()
                }
            }
        })

        loadComments()
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("CommentFragment", "Fragment started")
    }

    private fun loadComments() {
        lifecycleScope.launch {
            val comments = withContext(Dispatchers.IO) {
                transaction {
                    Comments.select { Comments.postId eq postId }
                        .map {
                            Comment(
                                commentId = it[Comments.commentId],
                                postId = it[Comments.postId],
                                userId = it[Comments.userId],
                                username = it[Comments.username],
                                content = it[Comments.content],
                                sentiment = it[Comments.sentiment]
                            )
                        }
                }
            }
            if (comments.isNotEmpty()) {
                commentsAdapter.updateComments(comments)
            }
        }
    }

    private fun loadData() {
        isLoading = true
        lifecycleScope.launch {
            // Simulate network load with delay
            withContext(Dispatchers.IO) {
                val newComments = transaction {
                    Comments.select { Comments.postId eq postId }
                        .map {
                            Comment(
                                commentId = it[Comments.commentId],
                                postId = it[Comments.postId],
                                userId = it[Comments.userId],
                                username = it[Comments.username],
                                content = it[Comments.content],
                                sentiment = it[Comments.sentiment]
                            )
                        }
                }
                withContext(Dispatchers.Main) {
                    if (newComments.isNotEmpty()) {
                        commentsAdapter.updateComments(newComments)
                    }
                    isLoading = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the swipe listener when the view is destroyed
        fragmentContainerLayout.setOnTouchListener(null)
        // Remove the layout change listener
        fragmentContainerLayout.viewTreeObserver.removeOnGlobalLayoutListener { }
        Log.d("CommentFragment", "Swipe listener removed")
    }

    fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return fragmentContainerLayout.dispatchTouchEvent(event)
    }

}
