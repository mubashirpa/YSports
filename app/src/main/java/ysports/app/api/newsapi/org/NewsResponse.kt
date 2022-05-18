package ysports.app.api.newsapi.org

import com.google.gson.annotations.SerializedName

data class NewsResponse(
    @SerializedName("status") val status: String?,
    @SerializedName("totalResults") val totalResults: String?,
    @SerializedName("articles") val articles: ArrayList<Articles>?
)