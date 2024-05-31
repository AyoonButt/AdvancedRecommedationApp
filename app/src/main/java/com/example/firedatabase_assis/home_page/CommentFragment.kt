package com.example.firedatabase_assis.home_page

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            val view = inflater.inflate(R.layout.fragment_comment, container, false)
            commentsRecyclerView = view.findViewById(R.id.comment_recycler_view)
            commentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

            // Initialize the adapter with an empty list (or null if you prefer)
            commentsAdapter =
                CommentsAdapter(requireContext(), viewLifecycleOwner, listOf(), commentDao, postId)

            // Set the adapter to the RecyclerView
            commentsRecyclerView.adapter = commentsAdapter

            
            loadComments()

            return view
        } catch (e: Exception) {
            Log.e("CommentFragment", "Error inflating fragment: ${e.message}")
            e.printStackTrace()
            return null
        }
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
                // Initialize the adapter with the fetched comments
                commentsAdapter.updateComments(comments)
            }
        }
    }

}
