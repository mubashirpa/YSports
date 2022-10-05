package ysports.app.api.leagues

import com.google.gson.annotations.SerializedName

data class Leagues(
    @SerializedName("league_id") val leagueId: Int,
    @SerializedName("league_logo") val leagueLogo: String,
    @SerializedName("league_title") val leagueTitle: String,
    val url: String
)