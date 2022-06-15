package ysports.app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ysports.app.R
import ysports.app.api.newsapi.org.Articles
import ysports.app.databinding.ListItemNewsBannerBinding
import ysports.app.databinding.ListItemNewsBinding
import ysports.app.ui.news.NewsViewHolder

class NewsAdapter(
    private val context: Context,
    private val arrayList: ArrayList<Articles>
) : RecyclerView.Adapter<NewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        return when(viewType) {
            R.layout.list_item_news_banner -> NewsViewHolder.NewsBannerViewHolder(
                ListItemNewsBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            R.layout.list_item_news -> NewsViewHolder.NewsItemsViewHolder(
                ListItemNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("Invalid ViewType Provided")
        }
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        when(holder) {
            is NewsViewHolder.NewsBannerViewHolder -> holder.bind(context, arrayList[position].imageUrl ?: "", arrayList[position].title ?: "", arrayList[position].publishedTime ?: "")
            is NewsViewHolder.NewsItemsViewHolder -> holder.bind(context, arrayList[position].imageUrl ?: "", arrayList[position].title ?: "", arrayList[position].publishedTime ?: "")
        }
    }

    override fun getItemCount() = arrayList.size

    override fun getItemViewType(position: Int): Int {
        return when(position) {
            0 -> R.layout.list_item_news_banner
            else -> R.layout.list_item_news
        }
    }
}