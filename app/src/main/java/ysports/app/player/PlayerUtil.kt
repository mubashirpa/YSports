// Last updated on 01 Aug 2022

package ysports.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.Nullable
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.util.Util
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import ysports.app.PlayerActivity
import ysports.app.R
import ysports.app.api.fixture.Media
import java.util.*

class PlayerUtil {

    private var selectedAudioPosition = 0
    private var selectedAudioChoice = 0
    private var selectedSubPosition = 0
    private var selectedSubChoice = 0
    private var selectedVideoPosition = 1
    private var selectedVideoChoice = 1
    private var selectedSpeedPosition = 3
    private var selectedSpeedChoice = 3
    private var isVideoTrackAuto = true

    fun loadPlayer(context: Context, mediaItems: List<MediaItem>, preferExtensionDecoders: Boolean) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders)
        }
        IntentUtil.addToIntent(mediaItems, intent)
        context.startActivity(intent)
    }

    fun loadPlayer(context: Context, videoUri: Uri, title: String?, preferExtensionDecoders: Boolean) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders)
        }
        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title ?: videoUri.lastPathSegment.toString()).build())
            .build()
        val mediaItems: List<MediaItem> = ImmutableList.of(mediaItem)
        IntentUtil.addToIntent(mediaItems, intent)
        context.startActivity(intent)
    }

    fun createMediaItems(mediaList: ArrayList<Media>) : List<MediaItem> {
        if (mediaList.isEmpty()) return Collections.emptyList()

        var uri: Uri?
        var extension: String?
        var title: String?
        var subtitleUri: Uri? = null
        var subtitleMimeType: String?
        var subtitleLanguage: String?
        var drmUuid: UUID? = null
        var drmLicenseUri: String? = null
        val drmLicenseRequestHeaders: ImmutableMap<String, String> = ImmutableMap.of()
        var drmSessionForClearContent: Boolean
        var drmMultiSession: Boolean
        var drmForceDefaultLicenseUri: Boolean
        var clippingConfiguration = MediaItem.ClippingConfiguration.Builder()

        val mediaItems: MutableList<MediaItem> = ArrayList()
        val mediaItem = MediaItem.Builder()

        for (i in 0 until mediaList.size) {
            title = mediaList[i].name
            uri = Uri.parse(mediaList[i].uri)
            extension = mediaList[i].extension
            val clipStartPositionMs = mediaList[i].clipStartPositionMs
            if (clipStartPositionMs != null)
                clippingConfiguration.setStartPositionMs(clipStartPositionMs)
            val clipEndPositionMs = mediaList[i].clipEndPositionMs
            if (clipEndPositionMs != null)
                clippingConfiguration.setEndPositionMs(clipEndPositionMs)
            val adTagUri = mediaList[i].adTagUri
            if (adTagUri != null)
                mediaItem.setAdsConfiguration(MediaItem.AdsConfiguration.Builder(Uri.parse(adTagUri)).build())
            val drmScheme = mediaList[i].drmScheme
            if (drmScheme != null)
                drmUuid = Util.getDrmUuid(drmScheme)
            val licenseUri = mediaList[i].drmLicenseUri
            val licenseUrl = mediaList[i].drmLicenseUrl
            if (licenseUri != null)
                drmLicenseUri = licenseUri
            else if (licenseUrl != null)
                drmLicenseUri = licenseUrl
            drmSessionForClearContent = mediaList[i].drmSessionForClearContent == true
            drmMultiSession = mediaList[i].drmMultiSession == true
            drmForceDefaultLicenseUri = mediaList[i].drmForceDefaultLicenseUri == true
            val subtitle = mediaList[i].subtitleUri
            if (subtitle != null)
                subtitleUri = Uri.parse(subtitle)
            subtitleMimeType = mediaList[i].subtitleMimeType
            subtitleLanguage = mediaList[i].subtitleLanguage

            @Nullable val adaptiveMimeType = Util.getAdaptiveMimeTypeForContentType(
                if (TextUtils.isEmpty(extension)) Util.inferContentType(uri) else Util.inferContentTypeForExtension(extension!!)
            )
            mediaItem
                .setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                .setMimeType(adaptiveMimeType)
                .setClippingConfiguration(clippingConfiguration.build())
            if (drmUuid != null) {
                mediaItem
                    .setDrmConfiguration(MediaItem.DrmConfiguration.Builder(drmUuid)
                        .setLicenseUri(drmLicenseUri)
                        .setLicenseRequestHeaders(drmLicenseRequestHeaders)
                        .setForceSessionsForAudioAndVideoTracks(drmSessionForClearContent)
                        .setMultiSession(drmMultiSession)
                        .setForceDefaultLicenseUri(drmForceDefaultLicenseUri)
                        .build())
            }  else {
                Preconditions.checkState(
                    drmLicenseUri == null,
                    "drm_uuid is required if drm_license_uri is set."
                )
                Preconditions.checkState(
                    drmLicenseRequestHeaders.isEmpty(),
                    "drm_uuid is required if drm_key_request_properties is set."
                )
                Preconditions.checkState(
                    !drmSessionForClearContent,
                    "drm_uuid is required if drm_session_for_clear_content is set."
                )
                Preconditions.checkState(
                    !drmMultiSession,
                    "drm_uuid is required if drm_multi_session is set."
                )
                Preconditions.checkState(
                    !drmForceDefaultLicenseUri,
                    "drm_uuid is required if drm_force_default_license_uri is set."
                )
            }
            if (subtitleUri != null) {
                val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType(Preconditions.checkNotNull(subtitleMimeType) {
                        "subtitle_mime_type is required if subtitle_uri is set."
                    })
                    .setLanguage(subtitleLanguage)
                    .build()
                mediaItem.setSubtitleConfigurations(ImmutableList.of(subtitleConfiguration))
            }

            mediaItems.add(mediaItem.build())

            subtitleUri = null
            drmUuid = null
            drmLicenseUri = null
            clippingConfiguration = MediaItem.ClippingConfiguration.Builder()
        }
        return mediaItems
    }

    fun selectAudioTrack(
        context: Context,
        player: ExoPlayer?,
    ) {
        val tracks = player?.currentTracks ?: return

        val audioTrackGroups = ArrayList<Tracks.Group>()
        val audioList = ArrayList<String>()
        var tempPosition = 0

        audioList.add("Disable")

        for (trackGroup in tracks.groups) {
            val trackType: Int = trackGroup.type

            if (trackType == C.TRACK_TYPE_AUDIO) {
                audioTrackGroups.add(trackGroup)

                for (i in 0 until trackGroup.length) {
                    val isSupported = trackGroup.isTrackSupported(i)
                    val isSelected = trackGroup.isTrackSelected(i)
                    val trackFormat = trackGroup.getTrackFormat(i)

                    if (isSelected) {
                        selectedAudioPosition = tempPosition + 1
                        selectedAudioChoice = selectedAudioPosition
                    }
                    tempPosition++

                    var label = if (trackFormat.label != null) " - ${trackFormat.label}" else ""
                    label += if (!isSupported) " (Unsupported)" else ""
                    val list = Locale(trackFormat.language.toString()).displayLanguage + label

                    if (list.contains("und") || list.contains("null")) audioList.add("Audio track #${i + 1}") else audioList.add(list)
                }
            }
        }

        val tempTracks = audioList.toArray(arrayOfNulls<CharSequence>(audioList.size))
        MaterialAlertDialogBuilder(context)
            .setTitle("Audio Track")
            .setNeutralButton(context.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                selectedAudioPosition = selectedAudioChoice
                if (selectedAudioPosition == 0) {
                    setAudioTrack(player)
                } else {
                    setAudioTrack(player, audioTrackGroups[selectedAudioPosition - 1])
                }
            }
            .setSingleChoiceItems(tempTracks, selectedAudioPosition) { _, position ->
                selectedAudioChoice = position
            }
            .show()
    }

    private fun setAudioTrack(player: ExoPlayer?) {
        player?.trackSelectionParameters =
            player?.trackSelectionParameters!!
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
    }

    private fun setAudioTrack(
        player: ExoPlayer?,
        audioTrackGroup: Tracks.Group?
    ) {
        player?.trackSelectionParameters =
            player?.trackSelectionParameters!!
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(
                    TrackSelectionOverride(
                        audioTrackGroup?.mediaTrackGroup!!, 0))
                .build()
    }

    fun selectSubTrack(
        context: Context,
        player: ExoPlayer?
    ) {
        val tracks = player?.currentTracks ?: return

        val textTrackGroups = ArrayList<Tracks.Group>()
        val subtitlesList = ArrayList<String>()
        var tempPosition = 0

        subtitlesList.add("Disable")

        for (trackGroup in tracks.groups) {
            val trackType: Int = trackGroup.type

            if (trackType == C.TRACK_TYPE_TEXT) {
                textTrackGroups.add(trackGroup)

                for (i in 0 until trackGroup.length) {
                    val isSupported = trackGroup.isTrackSupported(i)
                    val isSelected = trackGroup.isTrackSelected(i)
                    val trackFormat = trackGroup.getTrackFormat(i)

                    if (isSelected) {
                        selectedSubPosition = tempPosition + 1
                        selectedSubChoice = selectedSubPosition
                    }
                    tempPosition++

                    var label = if (trackFormat.label != null) " - ${trackFormat.label}" else ""
                    label += if (!isSupported) " (Unsupported)" else ""
                    val list = Locale(trackFormat.language.toString()).displayLanguage + label

                    if (list.contains("und") || list.contains("null")) subtitlesList.add("Subtitle track #${i + 1}") else subtitlesList.add(list)
                }
            }
        }

        val tempTracks = subtitlesList.toArray(arrayOfNulls<CharSequence>(subtitlesList.size))
        MaterialAlertDialogBuilder(context)
            .setTitle("Subtitle")
            .setNeutralButton(context.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                selectedSubPosition = selectedSubChoice
                if (selectedSubPosition == 0) {
                    setSubTrack(player)
                } else {
                    setSubTrack(player, textTrackGroups[selectedSubPosition - 1])
                }
            }
            .setSingleChoiceItems(tempTracks, selectedSubPosition) { _, position ->
                selectedSubChoice = position
            }
            .show()
    }

    private fun setSubTrack(player: ExoPlayer?) {
        player?.trackSelectionParameters =
            player?.trackSelectionParameters!!
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
    }

    private fun setSubTrack(
        player: ExoPlayer?,
        textTrackGroup: Tracks.Group?
    ) {
        player?.trackSelectionParameters =
            player?.trackSelectionParameters!!
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(
                    TrackSelectionOverride(
                        textTrackGroup?.mediaTrackGroup!!, 0))
                .build()
    }

    fun selectVideoTrack(
        context: Context,
        player: ExoPlayer?
    ) {
        val tracks = player?.currentTracks ?: return

        var group: Tracks.Group? = null
        val videoList = ArrayList<String>()

        videoList.add("Disable")
        videoList.add("Auto")

        for (trackGroup in tracks.groups) {
            val trackType: Int = trackGroup.type

            if (trackType == C.TRACK_TYPE_VIDEO) {
                group = trackGroup

                for (i in 0 until trackGroup.length) {
                    val isSupported = trackGroup.isTrackSupported(i)
                    val isSelected = trackGroup.isTrackSelected(i)
                    val trackFormat = trackGroup.getTrackFormat(i)

                    if (isSelected) {
                        selectedVideoPosition = i + 2
                        selectedVideoChoice = selectedVideoPosition
                    }
                    if (isVideoTrackAuto) {
                        selectedVideoPosition = 1
                        selectedVideoChoice = 1
                    }
                    /*
                    bit to byte = * 0.000001
                    bit to MB = * 0.000000125
                    bit to GB = * 0.000000000125
                    MB to GB = * 0.001
                    */
                    var dataUsage = 0.0
                    if (trackFormat.bitrate != -1) {
                        dataUsage = (trackFormat.bitrate * 0.000000000125) * 3600
                    }
                    val dataUsageMessage = if (dataUsage != 0.0) String.format("(up to %.2f GB per hour)", dataUsage) else ""
                    val width = trackFormat.width
                    val height = trackFormat.height
                    val list = if (isSupported) "${getResolution("${width}x${height}")} $dataUsageMessage" else "${getResolution("${width}x${height}")} (Unsupported)"

                    videoList.add(list)
                }
            }
        }

        val tempTracks = videoList.toArray(arrayOfNulls<CharSequence>(videoList.size))
        MaterialAlertDialogBuilder(context)
            .setTitle("Quality")
            .setNeutralButton(context.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                selectedVideoPosition = selectedVideoChoice
                isVideoTrackAuto = false
                setVideoTrack(player, group, selectedVideoPosition)
            }
            .setSingleChoiceItems(tempTracks, selectedVideoPosition) { _, position ->
                selectedVideoChoice = position
            }
            .show()
    }

    private fun setVideoTrack(
        player: ExoPlayer?,
        videoTrackGroup: Tracks.Group?,
        trackIndex: Int
    ) {
        when (selectedVideoPosition) {
            0 -> {
                player?.trackSelectionParameters =
                    player?.trackSelectionParameters!!
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
                        .build()
            }
            1 -> {
                isVideoTrackAuto = true
                player?.trackSelectionParameters =
                    player?.trackSelectionParameters!!
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                        .clearOverride(videoTrackGroup?.mediaTrackGroup!!)
                        .build()
            }
            else -> {
                player?.trackSelectionParameters =
                    player?.trackSelectionParameters!!
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                        .setOverrideForType(
                            TrackSelectionOverride(
                                videoTrackGroup?.mediaTrackGroup!!, trackIndex - 2))
                        .build()
            }
        }
    }

    fun setPlaybackSpeed(
        context: Context,
        player: ExoPlayer?
    ) {
        if (player == null) return

        val speedList = arrayOf("0.25", "0.5", "0.75", "Normal", "1.25", "1.5", "1.75", "2")
        val speed = floatArrayOf(0.25F, 0.5F, 0.75F, 1F, 1.25F, 1.5F, 1.75F, 2F)

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.playback_speed))
            .setNeutralButton(context.getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                selectedSpeedPosition = selectedSpeedChoice
                player.setPlaybackSpeed(speed[selectedSpeedPosition])
            }
            .setSingleChoiceItems(speedList, selectedSpeedPosition) { _, position ->
                selectedSpeedChoice = position
            }
            .show()
    }

    private fun getResolution(aspectRatio: String) : String {
        return when(aspectRatio) {
            // 16:9 aspect ratio
            "426x240" -> "240p"
            "640x360" -> "360p"
            "854x480" -> "480p"
            "1280x720" -> "720p"
            "1920x1080" -> "1080p HD"
            "2560x1440" -> "1440p QHD"
            "3840x2160" -> "2160p 4K"
            "7680x4320" -> "4320p 8K"
            // More
            "256x144" -> "144p"
            "480x360" -> "360p"
            "640x480" -> "480p"
            "960x540" -> "540p"
            "3072x1728" -> "3K"
            "2880x1620" -> "3K UHD"
            "5120x2880" -> "2880p 5K"
            "6144x3456" -> "6k"
            else -> aspectRatio
        }
    }
}