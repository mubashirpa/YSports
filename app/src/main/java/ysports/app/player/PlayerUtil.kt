// Last updated on 27 Jul 2022

package ysports.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.ImmutableList
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
    private var maxVideoBitrate: Int = 0

    fun loadPlayer(context: Context, mediaItems: List<MediaItem>, preferExtensionDecoders: Boolean) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders)
        }
        IntentUtil.addToIntent(mediaItems, intent)
        context.startActivity(intent)
    }

    fun loadPlayer(context: Context, videoUri: Uri, preferExtensionDecoders: Boolean) {
        val intent = Intent(context, PlayerActivity::class.java).apply {
            putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, preferExtensionDecoders)
        }
        val mediaItems: List<MediaItem> = ImmutableList.of(
            MediaItem.fromUri(videoUri)
        )
        IntentUtil.addToIntent(mediaItems, intent)
        context.startActivity(intent)
    }

    fun createMediaItems(mediaList: ArrayList<Media>) : List<MediaItem> {
        if (mediaList.isEmpty()) return Collections.emptyList()
        val mediaItemsBuilder = MediaItem.Builder()
        val mediaItems: MutableList<MediaItem> = ArrayList()

        var name: String?
        var uri: String?
        var drmScheme: String?
        var drmLicenseUri: String?
        var drmSessionForClearContent: Boolean?
        var drmForceDefaultLicenseUri: Boolean?
        var adTagUri: String?
        var clipEndPositionMs: Int?
        var clipStartPositionMs: Int?
        var subtitleUri: String?
        var subtitleMimeType: String?
        var subtitleLanguage: String?

        for (i in 0 until mediaList.size) {
            name = mediaList[i].name
            uri = mediaList[i].uri
            drmScheme = mediaList[i].drmScheme
            drmLicenseUri = mediaList[i].drmLicenseUri
            drmSessionForClearContent = mediaList[i].drmSessionForClearContent
            drmForceDefaultLicenseUri = mediaList[i].drmForceDefaultLicenseUri
            adTagUri = mediaList[i].adTagUri
            clipEndPositionMs = mediaList[i].clipEndPositionMs
            clipStartPositionMs = mediaList[i].clipStartPositionMs
            subtitleUri = mediaList[i].subtitleUri
            subtitleMimeType = mediaList[i].subtitleMimeType
            subtitleLanguage = mediaList[i].subtitleLanguage

            if (!name.isNullOrEmpty()) mediaItemsBuilder.setMediaMetadata(MediaMetadata.Builder().setTitle(name).build())
            if (!uri.isNullOrEmpty()) mediaItemsBuilder.setUri(uri)
            if (!drmScheme.isNullOrEmpty()) {
                val drmConfigurationBuilder = MediaItem.DrmConfiguration.Builder(drmConfigScheme(drmScheme))
                    .setPlayClearContentWithoutKey(drmSessionForClearContent ?: false)
                    .setForceDefaultLicenseUri(drmForceDefaultLicenseUri ?: false)
                if (!drmLicenseUri.isNullOrEmpty()) drmConfigurationBuilder.setLicenseUri(drmLicenseUri)
                mediaItemsBuilder.setDrmConfiguration(drmConfigurationBuilder.build())
            }
            if (!adTagUri.isNullOrEmpty()) mediaItemsBuilder.setAdsConfiguration(MediaItem.AdsConfiguration.Builder(Uri.parse(adTagUri)).build())
            if (clipEndPositionMs != null) mediaItemsBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setEndPositionMs(clipEndPositionMs.toLong())
                    .build())
            if (clipStartPositionMs != null) mediaItemsBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStartPositionMs.toLong())
                    .build())
            if (!subtitleUri.isNullOrEmpty()) {
                val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUri))
                if (!subtitleMimeType.isNullOrEmpty()) subtitleConfiguration.setMimeType(subtitleMimeType)
                if (!subtitleLanguage.isNullOrEmpty()) subtitleConfiguration.setLanguage(subtitleLanguage)
                mediaItemsBuilder.setSubtitleConfigurations(ImmutableList.of(subtitleConfiguration.build()))
            }
            mediaItems.add(mediaItemsBuilder.build())
        }
        return mediaItems
    }

    private fun drmConfigScheme(scheme: String) : UUID {
        if (scheme == "widevine") return C.WIDEVINE_UUID
        if (scheme == "playready") return C.PLAYREADY_UUID
        return C.UUID_NIL
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
                    if (trackFormat.bitrate > maxVideoBitrate) maxVideoBitrate = trackFormat.bitrate
                    var bitrate = ""
                    if (trackFormat.bitrate != -1) {
                        bitrate = String.format("- %.2f Mbps", trackFormat.bitrate * 0.000001)
                    }
                    val width = trackFormat.width
                    val height = trackFormat.height
                    val list = if (isSupported) "${width}x$height $bitrate" else "${width}x$height $bitrate (Unsupported)"

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
                        .setMaxVideoSizeSd()
                        .setMaxVideoBitrate(maxVideoBitrate)
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
}