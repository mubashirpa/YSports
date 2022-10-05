package ysports.app.api.matches

import com.google.gson.annotations.SerializedName

data class Matches(
    @SerializedName("away_team") val awayTeam: String,
    @SerializedName("away_team_logo") val awayTeamLogo: String,
    @SerializedName("away_team_score") val awayTeamScore: Int?,
    @SerializedName("home_team") val homeTeam: String,
    @SerializedName("home_team_logo") val homeTeamLogo: String,
    @SerializedName("home_team_score") val homeTeamScore: Int?,
    @SerializedName("league_name") val leagueName: String?,
    val media: Media?,
    val timestamp: String,
    val url: String?
)