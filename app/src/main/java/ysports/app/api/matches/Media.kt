package ysports.app.api.matches

import com.google.gson.annotations.SerializedName

data class Media(
    @SerializedName("ad_tag_uri") val adTagUri: String?,
    @SerializedName("clip_end_position_ms") val clipEndPositionMs: Long?,
    @SerializedName("clip_start_position_ms") val clipStartPositionMs: Long?,
    @SerializedName("drm_force_default_license_uri") val drmForceDefaultLicenseUri: Boolean?,
    @SerializedName("drm_license_uri") val drmLicenseUri: String?,
    @SerializedName("drm_license_url") val drmLicenseUrl: String?,
    @SerializedName("drm_multi_session") val drmMultiSession: Boolean?,
    @SerializedName("drm_scheme") val drmScheme: String?,
    @SerializedName("drm_session_for_clear_content") val drmSessionForClearContent: Boolean?,
    val extension: String?,
    val name: String?,
    val playlist: List<Playlist>?,
    @SerializedName("subtitle_language") val subtitleLanguage: String?,
    @SerializedName("subtitle_mime_type") val subtitleMimeType: String?,
    @SerializedName("subtitle_uri") val subtitleUri: String?,
    val uri: String?,
)