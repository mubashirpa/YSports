package ysports.app.api.leagues

import com.google.gson.annotations.SerializedName

data class LeaguesResponse(
    @SerializedName("leagues") val leagues: ArrayList<Leagues>?
)