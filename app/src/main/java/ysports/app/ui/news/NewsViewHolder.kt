package ysports.app.ui.news

import android.content.Context
import android.text.format.DateUtils
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import ysports.app.databinding.ListItemNewsBannerBinding
import ysports.app.databinding.ListItemNewsBinding
import java.text.SimpleDateFormat
import java.util.*

sealed class NewsViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    val calendar: Calendar = Calendar.getInstance()

    class NewsBannerViewHolder(private val binding: ListItemNewsBannerBinding) : NewsViewHolder(binding) {
        fun bind(context: Context, backdropPath: String?, title: String?, publishedTime: String?) {
            Glide.with(context)
                .load(backdropPath)
                .into(binding.backdrop)
            binding.title.text = title
            if (publishedTime?.isNotEmpty() == true) {
                calendar.time = simpleDateFormat.parse(publishedTime) as Date
                binding.time.text = DateUtils.getRelativeTimeSpanString(calendar.timeInMillis)
            }
        }
    }

    class NewsItemsViewHolder(private val binding: ListItemNewsBinding) : NewsViewHolder(binding) {
        fun bind(context: Context, backdropPath: String?, title: String?, publishedTime: String?) {
            Glide.with(context)
                .load(backdropPath)
                .into(binding.backdrop as ImageView)
            binding.title.text = title
            if (publishedTime?.isNotEmpty() == true) {
                calendar.time = simpleDateFormat.parse(publishedTime) as Date
                binding.time.text = DateUtils.getRelativeTimeSpanString(calendar.timeInMillis)
            }
        }
    }
}