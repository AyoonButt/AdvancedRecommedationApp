package com.example.firedatabase_assis.search

import android.view.View
import androidx.fragment.app.FragmentManager
import com.example.firedatabase_assis.R

class NavigationManager(private val fragmentManager: FragmentManager) {
    fun handleNavigationState(state: SearchViewModel.NavigationState) {
        when (state) {
            is SearchViewModel.NavigationState.ShowPoster -> {
                val tag = when (val item = state.item) {
                    is Movie -> "poster_movie_${item.id}"
                    is TV -> "poster_tv_${item.id}"
                    else -> "poster_${System.currentTimeMillis()}"
                }
                fragmentManager.beginTransaction()
                    .replace(R.id.container, PosterFragment.newInstance(state.item), tag)
                    .addToBackStack(tag)
                    .commit()
            }

            is SearchViewModel.NavigationState.ShowPerson -> {
                val tag = "person_${state.id}"
                fragmentManager.beginTransaction()
                    .replace(R.id.container, PersonDetailFragment.newInstance(state.id), tag)
                    .addToBackStack(tag)
                    .commit()
            }

            is SearchViewModel.NavigationState.Back -> fragmentManager.popBackStack()
            is SearchViewModel.NavigationState.Close -> clearStack()
        }
    }

    fun handleNavigation(event: SearchViewModel.NavigationState) {
        handleNavigationState(event)
        fragmentManager.findFragmentById(R.id.container)?.view?.visibility =
            if (event is SearchViewModel.NavigationState.Close ||
                (event is SearchViewModel.NavigationState.Back && isRootFragment())
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    fun isRootFragment(): Boolean {
        return fragmentManager.backStackEntryCount <= 1
    }

    private fun clearStack() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}