package ysports.app.api.fixture

import com.google.gson.annotations.SerializedName

data class Media(
    @SerializedName("name") val name: String?,
    @SerializedName("uri") val uri: String?,
    @SerializedName("drm_scheme") val drmScheme: String?,
    @SerializedName("drm_license_uri") val drmLicenseUri: String?,
    @SerializedName("drm_session_for_clear_content") val drmSessionForClearContent: Boolean?,
    @SerializedName("drm_force_default_license_uri") val drmForceDefaultLicenseUri: Boolean?,
    @SerializedName("ad_tag_uri") val adTagUri: String?,
    @SerializedName("clip_end_position_ms") val clipEndPositionMs: Int?,
    @SerializedName("clip_start_position_ms") val clipStartPositionMs: Int?,
    @SerializedName("subtitle_uri") val subtitleUri: String?,
    @SerializedName("subtitle_mime_type") val subtitleMimeType: String?,
    @SerializedName("subtitle_language") val subtitleLanguage: String?,
)