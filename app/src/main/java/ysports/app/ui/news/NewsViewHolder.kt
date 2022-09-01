package ysports.app.ui.news

import android.content.Context
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import ysports.app.R
import ysports.app.databinding.ListItemNewsBannerBinding
import ysports.app.databinding.ListItemNewsBinding

sealed class NewsViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

    class NewsBannerViewHolder(private val binding: ListItemNewsBannerBinding) : NewsViewHolder(binding) {
        fun bind(context: Context, backdropPath: String?, title: String?, publishedTime: String?) {
            Glide.with(context)
                .load(backdropPath)
                .placeholder(R.drawable.bg_image_placeholder_8dp)
                .into(binding.backdrop as ImageView)
            binding.title.text = title
            if (publishedTime?.isNotEmpty() == true) {
                var time = publishedTime
                if (time.contains("T")) time = time.replace("T", " ")
                if (time.contains("Z")) time = time.replace("Z", "")
                binding.time.text = time
            }
        }
    }

    class NewsItemsViewHolder(private val binding: ListItemNewsBinding) : NewsViewHolder(binding) {
        fun bind(context: Context, backdropPath: String?, title: String?, publishedTime: String?) {
            Glide.with(context)
                .load(backdropPath)
                .placeholder(R.drawable.bg_image_placeholder_8dp)
                .into(binding.backdrop as ImageView)
            binding.title.text = title
            if (publishedTime?.isNotEmpty() == true) {
                var time = publishedTime
                if (time.contains("T")) time = time.replace("T", " ")
                if (time.contains("Z")) time = time.replace("Z", "")
                binding.time.text = time
            }
        }
    }
}