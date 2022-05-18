// Last updated on 09 Apr 2022

package ysports.app.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
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
        tracksInfo: TracksInfo?,
        trackSelector: DefaultTrackSelector?
    ) {
        if (tracksInfo == null || trackSelector == null) return

        val audioTrack = ArrayList<String>()
        val audioList = ArrayList<String>()

        audioTrack.add("none")
        audioList.add("Disable")

        for (groupInfo: TracksInfo.TrackGroupInfo in tracksInfo.trackGroupInfos) {
            val trackType = groupInfo.trackType
            //val trackInGroupIsSelected = groupInfo.isSelected
            //val trackInGroupIsSupported = groupInfo.isSupported
            val group = groupInfo.trackGroup

            if (trackType == C.TRACK_TYPE_AUDIO) {
                for (i in 0 until group.length) {
                    val isSupported = groupInfo.isTrackSupported(i)
                    val isSelected = groupInfo.isTrackSelected(i)
                    val trackFormat = group.getFormat(i)

                    if (isSelected) {
                        selectedAudioPosition = i + 1
                        selectedAudioChoice = selectedAudioPosition
                    }
                    var label = if (trackFormat.label != null) " - ${trackFormat.label}" else ""
                    label += if (!isSupported) " (Unsupported)" else ""
                    val list = Locale(trackFormat.language.toString()).displayLanguage + label

                    audioTrack.add(trackFormat.language.toString())
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
                    trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setRendererDisabled(C.TRACK_TYPE_AUDIO, true))
                } else {
                    setAudioTrack(trackSelector, audioTrack[selectedAudioPosition])
                }
            }
            .setSingleChoiceItems(tempTracks, selectedAudioPosition) { _, position ->
                selectedAudioChoice = position
            }
            .show()
    }

    private fun setAudioTrack(
        trackSelector: DefaultTrackSelector?,
        audioLanguage: String?
    ) {
        trackSelector?.setParameters(
            trackSelector.buildUponParameters()
                .setRendererDisabled(C.TRACK_TYPE_AUDIO, false)
                .setPreferredAudioLanguage(audioLanguage)
        )
    }

    fun selectSubTrack(
        context: Context,
        tracksInfo: TracksInfo?,
        trackSelector: DefaultTrackSelector?
    ) {
        if (tracksInfo == null || trackSelector == null) return

        val subtitles = ArrayList<String>()
        val subtitlesList = ArrayList<String>()

        subtitles.add("none")
        subtitlesList.add("Disable")

        for (groupInfo: TracksInfo.TrackGroupInfo in tracksInfo.trackGroupInfos) {
            val trackType = groupInfo.trackType
            //val trackInGroupIsSelected = groupInfo.isSelected
            //val trackInGroupIsSupported = groupInfo.isSupported
            val group = groupInfo.trackGroup

            if (trackType == C.TRACK_TYPE_TEXT) {
                for (i in 0 until group.length) {
                    val isSupported = groupInfo.isTrackSupported(i)
                    val isSelected = groupInfo.isTrackSelected(i)
                    val trackFormat: Format = group.getFormat(i)

                    if (isSelected) {
                        selectedSubPosition = i + 1
                        selectedSubChoice = selectedSubPosition
                    }
                    var label = if (trackFormat.label != null) " - ${trackFormat.label}" else ""
                    label += if (!isSupported) " (Unsupported)" else ""

                    subtitles.add(trackFormat.language.toString())
                    subtitlesList.add(Locale(trackFormat.language.toString()).displayLanguage + label)
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
                    trackSelector.setParameters(trackSelector.buildUponParameters()
                        .setRendererDisabled(C.TRACK_TYPE_VIDEO, true))
                } else {
                    setSubTrack(trackSelector, subtitles[selectedSubPosition])
                }
            }
            .setSingleChoiceItems(tempTracks, selectedSubPosition) { _, position ->
                selectedSubChoice = position
            }
            .show()
    }

    private fun setSubTrack(
        trackSelector: DefaultTrackSelector?,
        subLanguage: String
    ) {
        trackSelector?.setParameters(trackSelector.buildUponParameters()
            .setRendererDisabled(C.TRACK_TYPE_VIDEO, false)
            .setPreferredTextLanguage(subLanguage))
    }

    fun selectVideoTrack(
        context: Context,
        tracksInfo: TracksInfo?,
        trackSelector: DefaultTrackSelector?
    ) {
        if (tracksInfo == null || trackSelector == null) return

        val videoWidth = ArrayList<Int>()
        val videoHeight = ArrayList<Int>()
        val videoList = ArrayList<String>()

        videoWidth.add(0)
        videoHeight.add(0)
        videoList.add("Disable")
        videoWidth.add(0)
        videoHeight.add(0)
        videoList.add("Auto")

        for (groupInfo: TracksInfo.TrackGroupInfo in tracksInfo.trackGroupInfos) {
            val trackType = groupInfo.trackType
            //val trackInGroupIsSelected = groupInfo.isSelected
            //val trackInGroupIsSupported = groupInfo.isSupported
            val group = groupInfo.trackGroup

            if (trackType == C.TRACK_TYPE_VIDEO) {
                for (i in 0 until group.length) {
                    val isSupported = groupInfo.isTrackSupported(i)
                    val isSelected = groupInfo.isTrackSelected(i)
                    val trackFormat: Format = group.getFormat(i)

                    if (isSelected) {
                        selectedVideoPosition = i + 2
                        selectedVideoChoice = selectedVideoPosition
                    }
                    if (isVideoTrackAuto) {
                        selectedVideoPosition = 1
                        selectedVideoChoice = 1
                    }
                    var bitrate = ""
                    if (trackFormat.bitrate != -1) {
                        bitrate = String.format("- %.2f Mbps", trackFormat.bitrate * 0.000001)
                    }
                    val width = trackFormat.width
                    val height = trackFormat.height
                    val list = if (isSupported) "${width}x$height $bitrate" else "${width}x$height $bitrate (Unsupported)"

                    videoWidth.add(width)
                    videoHeight.add(height)
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
                when (selectedVideoPosition) {
                    0 -> {
                        trackSelector.setParameters(trackSelector.buildUponParameters()
                            .setDisabledTrackTypes(ImmutableSet.of(C.TRACK_TYPE_VIDEO)))
                    }
                    1 -> {
                        isVideoTrackAuto = true
                        trackSelector.setParameters(trackSelector.buildUponParameters()
                            .setDisabledTrackTypes(ImmutableSet.of())
                            .setMaxVideoSizeSd())
                    }
                    else -> {
                        setVideoTrack(trackSelector, videoWidth[selectedVideoPosition], videoHeight[selectedVideoPosition])
                    }
                }
            }
            .setSingleChoiceItems(tempTracks, selectedVideoPosition) { _, position ->
                selectedVideoChoice = position
            }
            .show()
    }

    private fun setVideoTrack(
        trackSelector: DefaultTrackSelector?,
        width: Int,
        height: Int
    ) {
        trackSelector?.setParameters(trackSelector.buildUponParameters()
            .setDisabledTrackTypes(ImmutableSet.of())
            .setMaxVideoSize(width, height))
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