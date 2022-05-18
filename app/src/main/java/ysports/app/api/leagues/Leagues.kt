package ysports.app.api.leagues

import com.google.gson.annotations.SerializedName

data class Leagues(
    @SerializedName("league_id") val id: String?,
    @SerializedName("league_title") val title: String?,
    @SerializedName("league_title_alternate") val alternateTitle: String?,
    @SerializedName("club_logo") val logo: String?,
    @SerializedName("url") val url: String?
)