package ysports.app.api.fixture

import com.google.gson.annotations.SerializedName

data class Fixtures(
    @SerializedName("homeTeam") val homeTeam: String?,
    @SerializedName("homeTeamScore") val homeTeamScore: String?,
    @SerializedName("homeTeamLogo") val homeTeamLogo: String?,
    @SerializedName("awayTeam") val awayTeam: String?,
    @SerializedName("awayTeamScore") val awayTeamScore: String?,
    @SerializedName("awayTeamLogo") val awayTeamLogo: String?,
    @SerializedName("leagueName") val leagueName: String?,
    @SerializedName("matchDate") val matchDate: String?,
    @SerializedName("matchTime") val matchTime: String?,
    @SerializedName("media") val media: ArrayList<Media>?,
    @SerializedName("url") val url: String?
)