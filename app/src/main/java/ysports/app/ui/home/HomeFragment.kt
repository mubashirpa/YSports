package ysports.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ysports.app.R
import ysports.app.WebActivity
import ysports.app.YouTubePlayerActivity
import ysports.app.adapter.LeaguesAdapter
import ysports.app.adapter.MatchesAdapter
import ysports.app.adapter.NewsAdapter
import ysports.app.api.JsonApi
import ysports.app.api.fixture.FixtureResponse
import ysports.app.api.fixture.Fixtures
import ysports.app.api.fixture.Media
import ysports.app.api.leagues.Leagues
import ysports.app.api.leagues.LeaguesResponse
import ysports.app.api.newsapi.org.Articles
import ysports.app.api.newsapi.org.NewsApi
import ysports.app.api.newsapi.org.NewsResponse
import ysports.app.databinding.FragmentHomeBinding
import ysports.app.player.PlayerUtil
import ysports.app.util.AppUtil
import ysports.app.util.RecyclerDecorationHorizontal
import ysports.app.util.RecyclerTouchListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var leaguesRecyclerView: RecyclerView
    private lateinit var progressBarLeagues: CircularProgressIndicator
    private lateinit var leaguesErrorLayout: View
    private lateinit var leaguesRetryButton: Button
    private lateinit var matchesRecyclerView: RecyclerView
    private lateinit var progressBarMatches: CircularProgressIndicator
    private lateinit var matchesErrorLayout: View
    private lateinit var matchesRetryButton: Button
    private lateinit var newsRecyclerView: RecyclerView
    private lateinit var newsStateDescription: TextView
    private var leaguesApi: Call<LeaguesResponse>? = null
    private var matchesApi: Call<FixtureResponse>? = null
    private var newsApi: Call<NewsResponse>? = null
    private lateinit var appUtil: AppUtil

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaguesRecyclerView = binding.recyclerViewLeagues
        progressBarLeagues = binding.progressBarLeagues
        leaguesErrorLayout = view.findViewById(R.id.leagues_error_layout)
        leaguesRetryButton = binding.leaguesErrorLayout.buttonRetry
        matchesRecyclerView = binding.recyclerViewMatches
        progressBarMatches = binding.progressBarMatches
        matchesErrorLayout = view.findViewById(R.id.matches_error_layout)
        matchesRetryButton = binding.matchesErrorLayout.buttonRetry
        newsRecyclerView = binding.recyclerViewNews
        newsStateDescription = binding.newsStateDescription
        appUtil = AppUtil(requireContext())

        leaguesRetryButton.setOnClickListener {
            leaguesErrorLayout.hideView()
            progressBarLeagues.showView()
            Timer().schedule(500) {
                readLeaguesDB()
            }
        }

        matchesRetryButton.setOnClickListener {
            matchesErrorLayout.hideView()
            progressBarMatches.showView()
            Timer().schedule(500) {
                readMatchesDB()
            }
        }

        readLeaguesDB()
        readMatchesDB()
        readNewsDB()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        leaguesApi?.cancel()
        matchesApi?.cancel()
        newsApi?.cancel()
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun readLeaguesDB() {
        leaguesApi = JsonApi.create("https://api.npoint.io/").getLeagues("ef26b2579a1e6fba29fe")
        leaguesApi!!.enqueue(object : Callback<LeaguesResponse> {
            override fun onResponse(call: Call<LeaguesResponse>, response: Response<LeaguesResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(leaguesRecyclerView, progressBarLeagues, leaguesErrorLayout, leaguesRetryButton, R.string.error_retrofit_response, false)
                    return
                }
                val leaguesList: ArrayList<Leagues> = response.body()?.leagues ?: ArrayList()
                if (leaguesList.isEmpty()) {
                    errorOccurred(leaguesRecyclerView, progressBarLeagues, leaguesErrorLayout, leaguesRetryButton, R.string.error_default, false)
                    return
                }
                val leaguesAdapter = LeaguesAdapter(requireContext(), leaguesList)
                val itemDecoration = RecyclerDecorationHorizontal(20, 20, 10)
                leaguesRecyclerView.apply {
                    itemAnimator = DefaultItemAnimator()
                    addItemDecoration(itemDecoration)
                    adapter = leaguesAdapter
                    addOnItemTouchListener(
                        RecyclerTouchListener(context, leaguesRecyclerView, object : RecyclerTouchListener.ClickListener {
                            override fun onClick(view: View, position: Int) {
                                if (!leaguesList[position].url.isNullOrEmpty()) {
                                    val url: String = leaguesList[position].url!!
                                    when {
                                        url.startsWith("chrome://") -> {
                                            val replacedURL: String = url.replace("chrome://", "")
                                            appUtil.openCustomTabs(replacedURL)
                                        }
                                        else -> {
                                            val intent = Intent(context, WebActivity::class.java).apply {
                                                putExtra("WEB_URL", url)
                                            }
                                            startActivity(intent)
                                        }
                                    }
                                }
                            }

                            override fun onLongClick(view: View, position: Int) {

                            }
                        })
                    )
                }
                progressBarLeagues.hideView()
                leaguesRecyclerView.showView()
            }

            override fun onFailure(call: Call<LeaguesResponse>, t: Throwable) {
                if (!isDetached) {
                    errorOccurred(leaguesRecyclerView, progressBarLeagues, leaguesErrorLayout, leaguesRetryButton, R.string.error_failed_to_load_content, true)
                }
            }
        })
    }

    private fun readMatchesDB() {
        matchesApi = JsonApi.create("https://api.npoint.io/").getFixture("831085549ee2af13a198")
        matchesApi!!.enqueue(object : Callback<FixtureResponse> {
            override fun onResponse(call: Call<FixtureResponse>, response: Response<FixtureResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(matchesRecyclerView, progressBarMatches, matchesErrorLayout, matchesRetryButton, R.string.error_retrofit_response, false)
                    return
                }
                val matchList: ArrayList<Fixtures> = response.body()?.fixtures ?: ArrayList()
                val dateFormat = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
                val currentDate = Date()

                if (matchList.isEmpty()) {
                    errorOccurred(matchesRecyclerView, progressBarMatches, matchesErrorLayout, matchesRetryButton, R.string.error_no_matches, false)
                    return
                }
                val filteredList = matchList.filter {
                    !it.matchDate.isNullOrEmpty() && dateFormat.parse(it.matchDate)!! == currentDate
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(matchesRecyclerView, progressBarMatches, matchesErrorLayout, matchesRetryButton, R.string.error_no_matches_today, false)
                    return
                }
                val liveAdapter = MatchesAdapter(requireContext(),
                    filteredList as ArrayList<Fixtures>
                )
                val itemDecoration = RecyclerDecorationHorizontal(20, 20, 10)
                matchesRecyclerView.apply {
                    itemAnimator = DefaultItemAnimator()
                    addItemDecoration(itemDecoration)
                    adapter = liveAdapter
                    addOnItemTouchListener(
                        RecyclerTouchListener(context, matchesRecyclerView, object : RecyclerTouchListener.ClickListener {
                            override fun onClick(view: View, position: Int) {
                                if (!matchList[position].url.isNullOrEmpty()) {
                                    val url: String = matchList[position].url!!
                                    when {
                                        url.startsWith("chrome://") -> {
                                            val replacedURL: String = url.replace("chrome://", "")
                                            appUtil.openCustomTabs(replacedURL)
                                        }
                                        url.startsWith("video://") -> {
                                            val replacedURL = url.replace("video://", "")
                                            if (replacedURL.startsWith("https://youtu.be/")) {
                                                val intent = Intent(context, YouTubePlayerActivity::class.java).apply {
                                                    putExtra("VIDEO_URL", replacedURL)
                                                }
                                                startActivity(intent)
                                            }
                                        }
                                        else -> {
                                            val intent = Intent(context, WebActivity::class.java).apply {
                                                putExtra("WEB_URL", url)
                                            }
                                            startActivity(intent)
                                        }
                                    }
                                } else if (!matchList[position].media.isNullOrEmpty()) {
                                    val mediaList: ArrayList<Media> = matchList[position].media ?: ArrayList()
                                    val playerUtil = PlayerUtil()
                                    val mediaItems: List<MediaItem> = playerUtil.createMediaItems(
                                        mediaList
                                    )
                                    playerUtil.loadPlayer(context, mediaItems, true)
                                }
                            }

                            override fun onLongClick(view: View, position: Int) {

                            }
                        })
                    )
                }
                progressBarMatches.hideView()
                matchesRecyclerView.showView()
            }

            override fun onFailure(call: Call<FixtureResponse>, t: Throwable) {
                if (!isDetached) {
                    errorOccurred(matchesRecyclerView, progressBarMatches, matchesErrorLayout, matchesRetryButton, R.string.error_failed_to_load_content, true)
                }
            }
        })
    }

    private fun readNewsDB() {
        newsApi = NewsApi.create().getNews("top-headlines", getString(R.string.news_api_v2), "sports", "in")
        newsApi!!.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                if (!response.isSuccessful) {
                    newsStateDescription.text = getString(R.string.error_retrofit_response)
                    newsStateDescription.showView()
                    return
                }
                val newsList: ArrayList<Articles> = response.body()?.articles ?: ArrayList()
                if (newsList.isEmpty()) {
                    newsStateDescription.showView()
                    return
                }
                val newsAdapter = NewsAdapter(requireContext(), newsList)
                newsRecyclerView.apply {
                    itemAnimator = DefaultItemAnimator()
                    adapter = newsAdapter
                    addOnItemTouchListener(
                        RecyclerTouchListener(context, newsRecyclerView, object : RecyclerTouchListener.ClickListener {
                            override fun onClick(view: View, position: Int) {
                                if (!newsList[position].url.isNullOrEmpty()) appUtil.openCustomTabs(newsList[position].url!!)
                            }

                            override fun onLongClick(view: View, position: Int) {

                            }
                        })
                    )
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                if (!isDetached) newsStateDescription.showView()
            }
        })
    }

    private fun errorOccurred(
        recyclerView: RecyclerView,
        progressIndicator: CircularProgressIndicator,
        errorLayout: View,
        retryButton: Button,
        error: Int,
        showButton: Boolean
    ) {
        progressIndicator.hideView()
        recyclerView.hideView()
        errorLayout.findViewById<TextView>(R.id.state_description).text = getString(error)
        retryButton.isVisible = showButton
        errorLayout.showView()
    }
}