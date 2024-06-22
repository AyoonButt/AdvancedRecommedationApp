package com.example.firedatabase_assis.login_setup


import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class PromptSpinnerAdapter<T>(
    context: Context,
    private val resource: Int,
    private val objects: List<T>,
    private val prompt: String
) : ArrayAdapter<T>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        if (position == 0) {
            (view as TextView).text = prompt
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        if (position == 0) {
            (view as TextView).text = prompt
        }
        return view
    }

    override fun isEnabled(position: Int): Boolean {
        return position != 0
    }
}
