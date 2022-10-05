package ysports.app.api.matches

import com.google.gson.annotations.SerializedName

data class Playlist(
    @SerializedName("ad_tag_uri") val adTagUri: String?,
    @SerializedName("clip_end_position_ms") val clipEndPositionMs: Long?,
    @SerializedName("clip_start_position_ms") val clipStartPositionMs: Long?,
    @SerializedName("drm_license_uri") val drmLicenseUri: String?,
    @SerializedName("drm_scheme") val drmScheme: String?,
    val uri: String
)