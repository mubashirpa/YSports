package ysports.app.ui.matches

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ysports.app.PrivateKeys
import ysports.app.R
import ysports.app.api.JsonApi
import ysports.app.api.fixture.FixtureResponse
import ysports.app.api.fixture.Fixtures
import ysports.app.databinding.FragmentMatchesBinding
import ysports.app.ui.matches.adapter.MatchesViewPagerAdapter
import ysports.app.util.NetworkUtil
import java.util.*
import kotlin.concurrent.schedule

/* Empty constructor is added due to no constructor found error occurred when app restarts */
class MatchesFragment() : Fragment() {

    constructor(appBar: AppBarLayout) : this() {
        this.appBar = appBar
    }

    private var _binding: FragmentMatchesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var errorLayout: View
    private lateinit var retryButton: Button
    private lateinit var tabLayoutContainer: LinearLayout
    private var appBar: AppBarLayout? = null
    private var fixtureApi: Call<FixtureResponse>? = null
    private var fixtureList: ArrayList<Fixtures> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewPager = binding.viewPager
        tabLayout = binding.tabLayout
        progressBar = binding.progressBar
        errorLayout = binding.errorLayout.root
        retryButton = binding.errorLayout.buttonRetry
        tabLayoutContainer = binding.tabLayoutContainer

        appBar?.addLiftOnScrollListener { _, backgroundColor ->
            tabLayoutContainer.setBackgroundColor(backgroundColor)
        }

        retryButton.setOnClickListener {
            errorLayout.hideView()
            progressBar.showView()
            Timer().schedule(500) {
                readDatabase()
            }
        }

        readDatabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        fixtureApi?.cancel()
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun readDatabase() {
        fixtureApi = JsonApi.create("https://api.npoint.io/").getFixture(PrivateKeys().matchesUrlPath())
        fixtureApi?.enqueue(object : Callback<FixtureResponse> {
            override fun onResponse(call: Call<FixtureResponse>, response: Response<FixtureResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(R.string.error_retrofit_response, true)
                    return
                }
                fixtureList = response.body()?.fixtures ?: ArrayList()
                if (fixtureList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches, false)
                    return
                }
                setViewPagerAdapter(fixtureList)
            }

            override fun onFailure(call: Call<FixtureResponse>, t: Throwable) {
                if (isAdded) {
                    val error = if (NetworkUtil().isOnline(context)) R.string.error_failed_to_load_content else R.string.error_no_network
                    errorOccurred(error, true)
                }
            }
        })
    }

    private fun setViewPagerAdapter(arrayList: ArrayList<Fixtures>) {
        val matchesViewPagerAdapter = MatchesViewPagerAdapter(this, arrayList)
        viewPager.adapter = matchesViewPagerAdapter
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.previous)
                1 -> tab.text = getString(R.string.today)
                2 -> tab.text = getString(R.string.upcoming)
                else -> tab.text = (position + 1).toString()
            }
        }.attach()
        viewPager.setCurrentItem(1, false)
        progressBar.hideView()
        binding.contentLayout.showView()
    }

    private fun errorOccurred(error: Int, showButton: Boolean) {
        binding.errorLayout.stateDescription.text = getString(error)
        retryButton.isVisible = showButton
        progressBar.hideView()
        errorLayout.showView()
    }
}