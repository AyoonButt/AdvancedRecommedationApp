package com.example.firedatabase_assis

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class CommunitiesActivity : AppCompatActivity() {

    private lateinit var adapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communities)

        val recyclerView = findViewById<RecyclerView>(R.id.post_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PostAdapter(getDummyPosts(), this)
        recyclerView.adapter = adapter

        val addButton = findViewById<Button>(R.id.add_new_post_button)
        addButton.setOnClickListener {
            showAddPostDialog()
        }
    }

    private fun showAddPostDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.new_post_layout, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Create a New Post")
            .setPositiveButton("Post", null)
            .setNegativeButton("Cancel", { dialog, _ -> dialog.dismiss() })
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = dialogView.findViewById<EditText>(R.id.new_post_title).text.toString()
            val content = dialogView.findViewById<EditText>(R.id.new_post_content).text.toString()
            val community =
                dialogView.findViewById<EditText>(R.id.new_post_community).text.toString()
            if (title.isNotEmpty() && content.isNotEmpty()) {
                val newPost = Post(title, content, "Current User", community, 0, 0)
                adapter.addPost(newPost)
                dialog.dismiss()
            } else {
                Toast.makeText(
                    this@CommunitiesActivity,
                    "Please fill out all fields",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun getDummyPosts(): MutableList<Post> {
        return mutableListOf(

            Post("Dune 2", "round 30 imax", "batsdune", "Dune", 10, 2),
            Post(
                "VENOM 2",
                "This gon be the first movie ever to sell 1 trillion tickets",
                "BIGVENOMFAN12",
                "Spiderverse",
                5,
                1
            ),
            Post(
                "BREAKING",
                "Look between the D and H on your keyboard",
                "kirawontmiss",
                "dumbtweets",
                2,
                0
            ),
            Post(
                "Lore Recap",
                "Why doesn't Batman just call the Justice League for help? Is he stupid?",
                "Encajado",
                "BatmanArkham",
                53000,
                23000
            ),
        )
    }

}
