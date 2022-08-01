package ysports.app.api.leagues

import com.google.gson.annotations.SerializedName

data class Leagues(
    @SerializedName("league_title") val title: String?,
    @SerializedName("league_logo") val logo: String?,
    @SerializedName("url") val url: String?
)