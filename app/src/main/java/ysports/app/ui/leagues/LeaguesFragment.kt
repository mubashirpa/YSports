package ysports.app.ui.leagues

import android.content.Intent
import android.content.res.Resources
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
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ysports.app.R
import ysports.app.WebActivity
import ysports.app.api.JsonApi
import ysports.app.api.leagues.Leagues
import ysports.app.api.leagues.LeaguesResponse
import ysports.app.databinding.FragmentLeaguesBinding
import ysports.app.util.AppUtil
import ysports.app.util.NetworkUtil
import ysports.app.util.PrivateKeys
import ysports.app.widgets.recyclerview.GridSpacingItemDecoration
import ysports.app.widgets.recyclerview.ItemTouchListener
import java.util.*
import kotlin.concurrent.schedule

class LeaguesFragment : Fragment() {

    private var _binding: FragmentLeaguesBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var errorView: View
    private lateinit var retryButton: Button
    private lateinit var stateDescription: TextView
    private lateinit var itemDecoration: GridSpacingItemDecoration
    private var leaguesApi: Call<LeaguesResponse>? = null
    private var leaguesList: List<Leagues> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaguesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.recyclerView
        progressBar = binding.progressBar
        errorView = binding.errorView.root
        retryButton = binding.errorView.buttonRetry
        stateDescription = binding.errorView.stateDescription
        itemDecoration = GridSpacingItemDecoration(2, 10, 0, includeEdge = true, isReverse = false)
        val appUtil = AppUtil(requireContext())
        val isTablet = appUtil.isTablet()

        retryButton.setOnClickListener {
            errorView.hideView()
            progressBar.showView()
            Timer().schedule(500) {
                readDatabase()
            }
        }

        recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(itemDecoration)
            addOnItemTouchListener(
                ItemTouchListener(context, recyclerView, object : ItemTouchListener.ClickListener {
                    override fun onClick(view: View, position: Int) {
                        val url = leaguesList[position].url
                        val intent = Intent(context, WebActivity::class.java).apply {
                            putExtra("WEB_URL", url)
                        }
                        startActivity(intent)
                    }

                    override fun onLongClick(view: View, position: Int) {
                    }

                })
            )
        }

        if (isTablet) binding.recyclerContainer.maxWidth = dpToPx(appUtil.minScreenWidth())
        readDatabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        leaguesApi?.cancel()
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun readDatabase() {
        leaguesApi = JsonApi.create("https://api.npoint.io/").getLeagues(PrivateKeys().leaguesUrlPath())
        leaguesApi?.enqueue(object : Callback<LeaguesResponse> {
            override fun onResponse(call: Call<LeaguesResponse>, response: Response<LeaguesResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(R.string.error_retrofit_response, true)
                    return
                }
                leaguesList = response.body()?.leagues ?: emptyList()
                if (leaguesList.isEmpty()) {
                    errorOccurred(R.string.error_no_leagues, false)
                    return
                }
                setRecyclerAdapter(leaguesList)
            }

            override fun onFailure(call: Call<LeaguesResponse>, t: Throwable) {
                if (isAdded) {
                    val error = if (NetworkUtil().isOnline(context)) R.string.error_failed_to_load_content else R.string.error_no_network
                    errorOccurred(error, true)
                }
            }
        })
    }

    private fun setRecyclerAdapter(list: List<Leagues>) {
        errorView.hideView()
        val leaguesAdapter = LeaguesAdapter(requireContext(), list)
        recyclerView.adapter = leaguesAdapter
        progressBar.hideView()
        recyclerView.showView()
    }

    private fun errorOccurred(error: Int, showButton: Boolean) {
        progressBar.hideView()
        recyclerView.hideView()
        stateDescription.text = getString(error)
        retryButton.isVisible = showButton
        errorView.showView()
    }

    private fun dpToPx(dps: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dps * density + 0.5f).toInt()
    }
}