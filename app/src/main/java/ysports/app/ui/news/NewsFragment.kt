package ysports.app.ui.news

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ysports.app.BrowserActivity
import ysports.app.BuildConfig
import ysports.app.R
import ysports.app.api.newsapi.org.Articles
import ysports.app.api.newsapi.org.NewsApi
import ysports.app.api.newsapi.org.NewsResponse
import ysports.app.databinding.FragmentNewsBinding
import ysports.app.util.AppUtil
import ysports.app.util.NetworkUtil
import ysports.app.widgets.recyclerview.ItemTouchListener
import java.util.*
import kotlin.concurrent.schedule

class NewsFragment : Fragment() {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    private var newsApi: Call<NewsResponse>? = null
    private lateinit var errorView: View
    private lateinit var stateDescription: TextView
    private lateinit var retryButton: Button
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        errorView = view.findViewById(R.id.errorView)
        stateDescription = binding.errorView.stateDescription
        retryButton = binding.errorView.buttonRetry
        progressBar = binding.progressBar
        recyclerView = binding.recyclerView
        val appUtil = AppUtil(requireContext())
        val isTablet = appUtil.isTablet()

        retryButton.setOnClickListener {
            errorView.hideView()
            progressBar.showView()
            Timer().schedule(500) {
                readNewsDB()
            }
        }

        if (isTablet) binding.recyclerContainer.maxWidth = appUtil.minScreenWidth()
        readNewsDB()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        newsApi?.cancel()
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun readNewsDB() {
        val isTablet = AppUtil(requireContext()).isTablet()
        newsApi = NewsApi.create().getNews("top-headlines", BuildConfig.news_api_v2, "sports", "in")
        newsApi!!.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(R.string.error_retrofit_response, true)
                    return
                }
                val newsList: ArrayList<Articles> = response.body()?.articles ?: ArrayList()
                if (newsList.isEmpty()) {
                    errorOccurred(R.string.error_empty_response, false)
                    return
                }
                val newsAdapter = NewsAdapter(requireContext(), newsList)

                var recyclerLayoutManager = LinearLayoutManager(context)
                if (isTablet) {
                    recyclerLayoutManager = GridLayoutManager(context, 2)
                    recyclerLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                        override fun getSpanSize(position: Int): Int {
                            return if (position == 0) 2 else 1
                        }
                    }
                }

                recyclerView.apply {
                    itemAnimator = DefaultItemAnimator()
                    layoutManager = recyclerLayoutManager
                    adapter = newsAdapter
                    addOnItemTouchListener(
                        ItemTouchListener(context, recyclerView, object : ItemTouchListener.ClickListener {
                            override fun onClick(view: View, position: Int) {
                                if (!newsList[position].url.isNullOrEmpty()) {
                                    val intent = Intent(context, BrowserActivity::class.java).apply {
                                        putExtra("WEB_URL", newsList[position].url)
                                    }
                                    startActivity(intent)
                                }
                            }

                            override fun onLongClick(view: View, position: Int) {

                            }
                        })
                    )
                }
                progressBar.hideView()
                recyclerView.showView()
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                if (isAdded) {
                    val error = if (NetworkUtil().isOnline(context)) R.string.error_failed_to_load_content else R.string.error_no_network
                    errorOccurred(error, true)
                }
            }
        })
    }

    private fun errorOccurred(error: Int, showButton: Boolean) {
        progressBar.hideView()
        recyclerView.hideView()
        stateDescription.text = activity?.resources?.getString(error)
        retryButton.isVisible = showButton
        errorView.showView()
    }
}