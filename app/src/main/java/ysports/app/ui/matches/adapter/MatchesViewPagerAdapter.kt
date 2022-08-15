package ysports.app.ui.matches.adapter

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import ysports.app.api.fixture.Fixtures
import ysports.app.ui.matches.ARG_POSITION
import ysports.app.ui.matches.MatchesObjectFragment

private const val tabs_count = 3

class MatchesViewPagerAdapter(
    fragment: Fragment,
    private val arrayList: ArrayList<Fixtures>,
    private val viewPager: ViewPager2
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = tabs_count

    override fun createFragment(position: Int): Fragment {
        val fragment = MatchesObjectFragment(arrayList)
        fragment.arguments = Bundle().apply {
            putInt(ARG_POSITION, position)
        }
        //if (viewPager.currentItem == 0) viewPager.setCurrentItem(1, false)
        return fragment
    }
}