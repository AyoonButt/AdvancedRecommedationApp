package com.example.firedatabase_assis.search

import androidx.fragment.app.FragmentManager
import com.example.firedatabase_assis.R

class NavigationManager(private val fragmentManager: FragmentManager) {
    fun handleNavigation(state: SearchViewModel.NavigationState) {
        when (state) {
            is SearchViewModel.NavigationState.ShowPoster -> {
                val tag = if (state.isMovie) "poster_movie_${state.id}" else "poster_tv_${state.id}"
                fragmentManager.beginTransaction()
                    .replace(
                        R.id.container,
                        PosterFragment.newInstance(state.id, state.isMovie),
                        tag
                    )
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

    fun isRootFragment(): Boolean = fragmentManager.backStackEntryCount <= 1

    private fun clearStack() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
}