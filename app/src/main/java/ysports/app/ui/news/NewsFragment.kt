package ysports.app.ui.news

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ysports.app.BuildConfig
import ysports.app.R
import ysports.app.api.newsapi.org.Article
import ysports.app.api.newsapi.org.NewsApi
import ysports.app.api.newsapi.org.NewsResponse
import ysports.app.databinding.FragmentNewsBinding
import ysports.app.util.AppUtil
import ysports.app.util.NetworkUtil
import ysports.app.widgets.recyclerview.GridSpacingItemDecoration
import ysports.app.widgets.recyclerview.VerticalSpacingItemDecoration
import java.util.*
import kotlin.concurrent.schedule

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var errorView: View
    private lateinit var stateDescription: TextView
    private lateinit var retryButton: Button
    private var newsApi: Call<NewsResponse>? = null
    private var newsList: List<Article> = ArrayList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.recyclerView
        progressBar = binding.progressBar
        errorView = view.findViewById(R.id.errorView)
        stateDescription = binding.errorView.stateDescription
        retryButton = binding.errorView.buttonRetry
        val appUtil = AppUtil(requireContext())
        val isTablet = appUtil.isTablet()
        val marginMin =
            (resources.getDimension(R.dimen.margin_min) / resources.displayMetrics.density).toInt()
        val verticalItemDecoration = VerticalSpacingItemDecoration(marginMin, marginMin)
        val gridItemDecoration =
            GridSpacingItemDecoration(2, marginMin, 1, includeEdge = true, isReverse = false)

        retryButton.setOnClickListener {
            errorView.hideView()
            progressBar.showView()
            Timer().schedule(500) {
                readNewsDB()
            }
        }

        var recyclerLayoutManager = LinearLayoutManager(context)
        if (isTablet) {
            recyclerLayoutManager = GridLayoutManager(context, 2)
            recyclerLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == 0) 2 else 1
                }
            }
            recyclerView.addItemDecoration(gridItemDecoration)
        } else {
            recyclerView.addItemDecoration(verticalItemDecoration)
        }

        recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = recyclerLayoutManager
        }

        if (isTablet) binding.recyclerContainer.maxWidth = dpToPx(appUtil.minScreenWidth())
        readNewsDB()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        newsApi?.cancel()
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun readNewsDB() {
        newsApi = NewsApi.create().getNews("top-headlines", BuildConfig.news_api, "sports", "in")
        newsApi?.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(R.string.error_retrofit_response, true)
                    return
                }
                newsList = response.body()?.articles ?: ArrayList()
                if (newsList.isEmpty()) {
                    errorOccurred(R.string.error_empty_response, false)
                    return
                }
                val newsAdapter = NewsAdapter(requireContext(), newsList)
                recyclerView.adapter = newsAdapter
                progressBar.hideView()
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                if (isAdded) {
                    val error =
                        if (NetworkUtil().isOnline(context)) R.string.error_failed_to_load_content else R.string.error_no_network
                    errorOccurred(error, true)
                }
            }
        })
    }

    private fun errorOccurred(error: Int, showButton: Boolean) {
        progressBar.hideView()
        stateDescription.text = activity?.resources?.getString(error)
        retryButton.isVisible = showButton
        errorView.showView()
    }

    private fun dpToPx(dps: Int): Int {
        val density = Resources.getSystem().displayMetrics.density
        return (dps * density + 0.5f).toInt()
    }
}