package ysports.app.ui.news

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ysports.app.R
import ysports.app.api.newsapi.org.Article
import ysports.app.databinding.ListItemNewsBannerBetaBinding
import ysports.app.databinding.ListItemNewsBetaBinding

class NewsAdapter(
    private val context: Context, private val arrayList: List<Article>
) : RecyclerView.Adapter<NewsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        return when (viewType) {
            R.layout.list_item_news_banner -> NewsViewHolder.NewsBannerViewHolder(
                ListItemNewsBannerBetaBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
            R.layout.list_item_news -> NewsViewHolder.NewsItemsViewHolder(
                ListItemNewsBetaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> throw IllegalArgumentException("Invalid ViewType Provided")
        }
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        when (holder) {
            is NewsViewHolder.NewsBannerViewHolder -> holder.bind(
                context, arrayList, position
            )
            is NewsViewHolder.NewsItemsViewHolder -> holder.bind(
                context, arrayList, position
            )
        }
    }

    override fun getItemCount() = arrayList.size

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> R.layout.list_item_news_banner
            else -> R.layout.list_item_news
        }
    }
}