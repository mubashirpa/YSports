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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ysports.app.BrowserActivity
import ysports.app.R
import ysports.app.adapter.NewsAdapter
import ysports.app.api.newsapi.org.Articles
import ysports.app.api.newsapi.org.NewsApi
import ysports.app.api.newsapi.org.NewsResponse
import ysports.app.databinding.FragmentNewsBinding
import ysports.app.util.RecyclerTouchListener
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

        retryButton.setOnClickListener {
            errorView.hideView()
            progressBar.showView()
            Timer().schedule(500) {
                readNewsDB()
            }
        }

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
        newsApi = NewsApi.create().getNews("top-headlines", getString(R.string.news_api_v2), "sports", "in")
        newsApi!!.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(R.string.error_retrofit_response, false)
                    return
                }
                val newsList: ArrayList<Articles> = response.body()?.articles ?: ArrayList()
                if (newsList.isEmpty()) {
                    errorOccurred(R.string.error_empty_response, false)
                    return
                }
                val newsAdapter = NewsAdapter(requireContext(), newsList)
                recyclerView.apply {
                    itemAnimator = DefaultItemAnimator()
                    adapter = newsAdapter
                    addOnItemTouchListener(
                        RecyclerTouchListener(context, recyclerView, object : RecyclerTouchListener.ClickListener {
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
                if (!isDetached) errorOccurred(R.string.error_failed_to_load_content, true)
            }
        })
    }

    private fun errorOccurred(error: Int, showButton: Boolean) {
        progressBar.hideView()
        recyclerView.hideView()
        stateDescription.text = resources.getString(error)
        retryButton.isVisible = showButton
        errorView.showView()
    }
}