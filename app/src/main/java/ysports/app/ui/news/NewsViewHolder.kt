package ysports.app.ui.news

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.format.DateUtils
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import ysports.app.BrowserActivity
import ysports.app.R
import ysports.app.api.newsapi.org.Article
import ysports.app.databinding.ListItemNewsBannerBetaBinding
import ysports.app.databinding.ListItemNewsBetaBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

sealed class NewsViewHolder(binding: ViewBinding) : RecyclerView.ViewHolder(binding.root) {

    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    val calendar: Calendar = Calendar.getInstance()

    private fun saveImage(context: Context, bitmap: Bitmap): Uri? {
        val path = File(context.cacheDir, "images")
        if (!path.exists()) path.mkdirs()
        val file = File(path, "${abs(Random().nextLong())}.png").also {
            FileOutputStream(it).use { fileOutputStream ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG, 100, fileOutputStream
                )
            }
        }.apply {
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            context, context.getString(R.string.file_provider_authority), file
        )
    }

    fun shareText(context: Context, subject: String?, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooser: Intent = Intent.createChooser(sendIntent, null)
        if (sendIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(chooser)
        }
    }

    fun shareImageFromUrl(context: Context, urlToImage: String?, subject: String?, text: String) {
        Glide.with(context).asBitmap().load(urlToImage).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                val path = saveImage(context, resource)
                if (path != null) shareRichContent(context, path, subject, text)
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })
    }

    private fun shareRichContent(
        context: Context, contentUri: Uri?, subject: String?, text: String
    ) {
        val share = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, subject)
            data = contentUri?.normalizeScheme()
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }, null)
        context.startActivity(share)
    }

    class NewsBannerViewHolder(
        private val binding: ListItemNewsBannerBetaBinding
    ) : NewsViewHolder(binding) {
        fun bind(context: Context, arrayList: List<Article>, position: Int) {
            val title = arrayList[position].title
            val backdropPath = arrayList[position].urlToImage
            val publishedTime = arrayList[position].publishedAt
            val url = arrayList[position].url

            if (backdropPath != null) Glide.with(context).load(backdropPath).into(binding.backdrop)
            binding.source.text = arrayList[position].source.name.uppercase(Locale.ROOT)
            binding.title.text = title
            if (publishedTime.isNotEmpty()) {
                calendar.time = simpleDateFormat.parse(publishedTime) as Date
                binding.time.text = DateUtils.getRelativeTimeSpanString(calendar.timeInMillis)
            }

            binding.card.setOnClickListener {
                val intent = Intent(context, BrowserActivity::class.java).apply {
                    putExtra("WEB_URL", arrayList[position].url)
                }
                context.startActivity(intent)
            }
            binding.share.setOnClickListener {
                if (backdropPath != null) shareImageFromUrl(
                    context, backdropPath, title, url
                ) else shareText(context, title, url)
            }
        }
    }

    class NewsItemsViewHolder(
        private val binding: ListItemNewsBetaBinding
    ) : NewsViewHolder(binding) {
        fun bind(context: Context, arrayList: List<Article>, position: Int) {
            val title = arrayList[position].title
            val backdropPath = arrayList[position].urlToImage
            val publishedTime = arrayList[position].publishedAt
            val url = arrayList[position].url

            if (backdropPath != null) Glide.with(context).load(backdropPath).into(binding.backdrop)
            binding.source.text = arrayList[position].source.name.uppercase(Locale.ROOT)
            binding.title.text = title
            if (publishedTime.isNotEmpty()) {
                calendar.time = simpleDateFormat.parse(publishedTime) as Date
                binding.time.text = DateUtils.getRelativeTimeSpanString(calendar.timeInMillis)
            }

            binding.card.setOnClickListener {
                val intent = Intent(context, BrowserActivity::class.java).apply {
                    putExtra("WEB_URL", arrayList[position].url)
                }
                context.startActivity(intent)
            }
            binding.share.setOnClickListener {
                if (backdropPath != null) shareImageFromUrl(
                    context, backdropPath, title, url
                ) else shareText(context, title, url)
            }
        }
    }
}