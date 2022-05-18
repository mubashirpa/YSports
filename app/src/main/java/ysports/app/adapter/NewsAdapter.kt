package ysports.app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ysports.app.R
import ysports.app.api.newsapi.org.Articles

class NewsAdapter(
    private val context: Context,
    private val arrayList: ArrayList<Articles>
) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_home_latest_news, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context).load(arrayList[position].imageUrl ?: "").into(holder.backdrop)
        holder.title.text = arrayList[position].title ?: ""
        if (!arrayList[position].publishedTime.isNullOrEmpty()) {
            var time = arrayList[position].publishedTime!!
            if (time.contains("T")) time = time.replace("T", " ")
            if (time.contains("Z")) time = time.replace("Z", "")
            holder.time.text = time
        }
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val backdrop: ImageView = itemView.findViewById(R.id.backdrop)
        val title: TextView = itemView.findViewById(R.id.title)
        val time: TextView = itemView.findViewById(R.id.time)
    }
}