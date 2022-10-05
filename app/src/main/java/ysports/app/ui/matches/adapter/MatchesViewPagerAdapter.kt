package ysports.app.ui.matches.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ysports.app.api.matches.Matches
import ysports.app.ui.matches.ARG_POSITION
import ysports.app.ui.matches.MatchesObjectFragment

private const val tabs_count = 3

class MatchesViewPagerAdapter(
    fragment: Fragment,
    private val list: List<Matches>
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = tabs_count

    override fun createFragment(position: Int): Fragment {
        val fragment = MatchesObjectFragment(list)
        fragment.arguments = Bundle().apply {
            putInt(ARG_POSITION, position)
        }
        return fragment
    }
}