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
import com.example.firedatabase_assis.database.CommentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentFragment(private val postId: Int, private val commentDao: CommentDao) : Fragment() {

    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var commentsAdapter: CommentsAdapter
    private lateinit var swipeGestureListener: SwipeGestureListener
    private lateinit var fragmentContainerLayout: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("CommentFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_comment, container, false)
        commentsRecyclerView = view.findViewById(R.id.comment_recycler_view)
        commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        commentsAdapter =
            CommentsAdapter(requireContext(), viewLifecycleOwner, listOf(), commentDao, postId)
        commentsRecyclerView.adapter = commentsAdapter

        // Get the fragment container layout
        fragmentContainerLayout = requireActivity().findViewById(R.id.fragment_container)

        // Set up swipe gesture listener for the fragment_container layout
        swipeGestureListener = SwipeGestureListener(requireContext()) {
            Log.d("SwipeGestureListener", "Swipe down action triggered")
            fragmentContainerLayout.visibility = View.GONE
            requireActivity().supportFragmentManager.popBackStack()
        }

        fragmentContainerLayout.setOnTouchListener { _, event ->
            val result = swipeGestureListener.onTouch(fragmentContainerLayout, event)
            Log.d("CommentFragment", "Touch event: $event, result: $result")
            result
        }

        // Bring the fragment_container layout to the front
        fragmentContainerLayout.bringToFront()
        fragmentContainerLayout.requestFocus()

        // Log when the listener is attached
        Log.d("CommentFragment", "SwipeGestureListener attached to fragment_container layout")

        loadComments()
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("CommentFragment", "Fragment started")
    }

    private fun loadComments() {
        lifecycleScope.launch(Dispatchers.Main) {
            val comments = withContext(Dispatchers.IO) {
                commentDao.getCommentsForPost(postId)
            }
            if (comments.isNotEmpty()) {
                commentsAdapter.updateComments(comments)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Remove the swipe listener when the view is destroyed
        fragmentContainerLayout.setOnTouchListener(null)
        Log.d("CommentFragment", "Swipe listener removed")
    }

    fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return fragmentContainerLayout.dispatchTouchEvent(event)
    }
}
