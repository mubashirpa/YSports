package ysports.app.api.fixture

import com.google.gson.annotations.SerializedName

data class Fixtures(
    @SerializedName("home_team") val homeTeam: String?,
    @SerializedName("home_team_score") val homeTeamScore: String?,
    @SerializedName("home_team_logo") val homeTeamLogo: String?,
    @SerializedName("away_team") val awayTeam: String?,
    @SerializedName("away_team_score") val awayTeamScore: String?,
    @SerializedName("away_team_logo") val awayTeamLogo: String?,
    @SerializedName("league_name") val leagueName: String?,
    @SerializedName("match_date") val matchDate: String?,
    @SerializedName("match_time") val matchTime: String?,
    @SerializedName("media") val media: ArrayList<Media>?,
    @SerializedName("url") val url: String?
)