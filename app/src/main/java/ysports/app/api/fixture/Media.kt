package ysports.app.api.fixture

import com.google.gson.annotations.SerializedName

data class Media(
    @SerializedName("name") val name: String?,
    @SerializedName("uri") val uri: String?,
    @SerializedName("extension") val extension: String?,
    @SerializedName("clip_start_position_ms") val clipStartPositionMs: Long?,
    @SerializedName("clip_end_position_ms") val clipEndPositionMs: Long?,
    @SerializedName("ad_tag_uri") val adTagUri: String?,
    @SerializedName("drm_scheme") val drmScheme: String?,
    @SerializedName("drm_license_uri") val drmLicenseUri: String?,
    @SerializedName("drm_license_url") val drmLicenseUrl: String?,
    @SerializedName("drm_session_for_clear_content") val drmSessionForClearContent: Boolean?,
    @SerializedName("drm_multi_session") val drmMultiSession: Boolean?,
    @SerializedName("drm_force_default_license_uri") val drmForceDefaultLicenseUri: Boolean?,
    @SerializedName("subtitle_uri") val subtitleUri: String?,
    @SerializedName("subtitle_mime_type") val subtitleMimeType: String?,
    @SerializedName("subtitle_language") val subtitleLanguage: String?,
)