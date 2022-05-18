package ysports.app.api.newsapi.org

import com.google.gson.annotations.SerializedName

data class Articles(
    @SerializedName("title") val title: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("urlToImage") val imageUrl: String?,
    @SerializedName("publishedAt") val publishedTime: String?
)