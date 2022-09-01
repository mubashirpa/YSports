// Last updated on 27 Jul 2022
// Latest commit on May 23

/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ysports.app

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Pair
import android.util.Rational
import android.view.*
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.DefaultDrmSessionManagerProvider
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.ext.ima.ImaServerSideAdInsertionMediaSource
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.trackselection.TrackSelectionParameters
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.DebugTextViewHelper
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import com.google.android.material.chip.Chip
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import ysports.app.databinding.ActivityPlayerBinding
import ysports.app.ui.bottomsheet.PlayerMenuBottomSheet
import ysports.app.player.*
import ysports.app.player.IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA
import ysports.app.util.NotificationUtil
import java.util.*
import kotlin.math.abs
import kotlin.math.max

@Suppress("PrivatePropertyName")
class PlayerActivity : AppCompatActivity(), OnClickListener, StyledPlayerView.ControllerVisibilityListener,
    GestureDetector.OnGestureListener {

    // Saved instance state keys.

    private val KEY_TRACK_SELECTION_PARAMETERS = "track_selection_parameters"
    private val KEY_SERVER_SIDE_ADS_LOADER_STATE = "server_side_ads_loader_state"
    private val KEY_ITEM_INDEX = "item_index"
    private val KEY_POSITION = "position"
    private val KEY_AUTO_PLAY = "auto_play"

    private var playerView: StyledPlayerView? = null
    private var debugTextView: TextView? = null
    private var player: ExoPlayer? = null

    private var isShowingTrackSelectionDialog = false
    private var selectTracksButton: ImageButton? = null
    private var dataSourceFactory: DataSource.Factory? = null
    private var mediaItems: List<MediaItem>? = null
    private var trackSelectionParameters: TrackSelectionParameters? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var lastSeenTracks: Tracks? = null
    private var startAutoPlay = false
    private var startItemIndex = 0
    private var startPosition: Long = 0

    // For ad playback only.

    private var clientSideAdsLoader: AdsLoader? = null

    // TODO: Annotate this and serverSideAdsLoaderState below with @OptIn when it can be applied to
    // fields (needs http://r.android.com/2004032 to be released into a version of
    // androidx.annotation:annotation-experimental).
    private var serverSideAdsLoader: ImaServerSideAdInsertionMediaSource.AdsLoader? = null

    private var serverSideAdsLoaderState: @MonotonicNonNull ImaServerSideAdInsertionMediaSource.AdsLoader.State? = null

    /* END */

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var context: Context

    private lateinit var exoUnlock: ImageButton
    private lateinit var navigationButton: ImageButton
    private lateinit var exoPIP: ImageButton
    private lateinit var exoTitle: TextView
    private lateinit var exoPrevious: ImageButton
    private lateinit var exoNext: ImageButton
    private lateinit var liveIndicator: Chip
    private lateinit var exoLock: ImageButton
    private lateinit var changeAspectRatioButton: ImageButton
    private lateinit var exoPosition: TextView
    private lateinit var exoDuration: TextView

    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var playerNotificationManager: PlayerNotificationManager? = null
    private lateinit var audioManager: AudioManager
    private var broadcastReceiver: BroadcastReceiver? = null
    private val playerUtil = PlayerUtil()
    private var minSwipeY: Float = 0f
    private var brightness: Int = 0
    private var volume: Int = 0
    private val ACTION_PIP_MEDIA_CONTROL = "pip_media_control"
    private val PIP_REQUEST_CODE = 101
    private var PIP_ACTION_ICON_ID = R.drawable.ic_baseline_play_arrow_24
    private var playerLocked = false

    // Activity lifecycle.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        context = this
        dataSourceFactory = DemoUtil.getDataSourceFactory(context)

        setContentView()
        debugTextView = binding.debugTextView
        selectTracksButton = findViewById(R.id.exo_settings)
        selectTracksButton?.setOnClickListener(this)

        exoUnlock = findViewById(R.id.exo_unlock)
        navigationButton = findViewById(R.id.exo_navigation)
        exoPIP = findViewById(R.id.exo_pip)
        exoTitle = findViewById(R.id.exo_title)
        exoPrevious = findViewById(R.id.exo_prev)
        exoNext = findViewById(R.id.exo_next)
        liveIndicator = findViewById(R.id.exo_live_indicator)
        exoLock = findViewById(R.id.exo_lock)
        changeAspectRatioButton = findViewById(R.id.exo_change_aspect_ratio)
        exoPosition = findViewById(R.id.exo_position)
        exoDuration = findViewById(R.id.exo_duration)

        exoUnlock.setOnClickListener(this)
        navigationButton.setOnClickListener(this)
        exoPIP.setOnClickListener(this)
        liveIndicator.setOnClickListener(this)
        exoLock.setOnClickListener(this)
        changeAspectRatioButton.setOnClickListener(this)

        gestureDetectorCompat = GestureDetectorCompat(context, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        exoPIP.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        playerView = binding.playerView
        playerView?.setControllerVisibilityListener(this)
        playerView?.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView?.requestFocus()

        if (savedInstanceState != null) {
            trackSelectionParameters = TrackSelectionParameters.fromBundle(savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)!!)
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
            restoreServerSideAdsLoaderState(savedInstanceState)
        } else {
            trackSelectionParameters = TrackSelectionParameters.Builder( /* context= */this).build()
            clearStartPosition()
        }

        /* END */

        initializeSwipeControl()

        playerView?.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    updatePictureInPictureParams()
                }
            }
        }

        playerView?.setOnClickListener {
            if (playerLocked) exoUnlock.showView()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        releasePlayer()
        releaseClientSideAdsLoader()
        clearStartPosition()
        setIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
            if (playerView != null) {
                playerView!!.onResume()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer()
            if (playerView != null) {
                playerView!!.onResume()
            }
        }

        if(brightness != 0) setScreenBrightness(brightness)
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            if (playerView != null) {
                playerView!!.onPause()
            }
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            if (playerView != null) {
                playerView!!.onPause()
            }
            releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseClientSideAdsLoader()
    }

    /* END */

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            when {
                playerLocked -> {
                    showToast("Player is locked")
                    exoUnlock.showView()
                }
                else -> finish()
            }
        }
    }

    /* When user press home button */

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (player?.isPlaying == true) enterPictureInPicture()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) {
            // Empty results are triggered if a permission is requested while another request was already
            // pending and can be safely ignored in this case.
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer()
        } else {
            showToast(R.string.storage_permission_denied)
            finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        updateTrackSelectorParameters()
        updateStartPosition()
        outState.putBundle(KEY_TRACK_SELECTION_PARAMETERS, trackSelectionParameters?.toBundle())
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay)
        outState.putInt(KEY_ITEM_INDEX, startItemIndex)
        outState.putLong(KEY_POSITION, startPosition)
        saveServerSideAdsLoaderState(outState)
    }

    // Activity input

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView!!.dispatchKeyEvent(event!!) || super.dispatchKeyEvent(event)
    }

    // OnClickListener methods

    override fun onClick(v: View?) {
        when (v) {
            selectTracksButton -> openSettings()
            navigationButton -> onBackPressedCallback.handleOnBackPressed()
            exoPIP -> enterPictureInPicture()
            changeAspectRatioButton -> {
                if (playerView!!.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                    playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                } else {
                    playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
            liveIndicator -> {
                if (player?.isCurrentMediaItemLive!! && player?.isCurrentMediaItemDynamic!!) {
                    player?.seekToDefaultPosition()
                    liveIndicator.text = resources.getString(R.string.live_caps)
                }
            }
            exoLock -> lockPlayer()
            exoUnlock -> unlockPlayer()
        }
    }

    // StyledPlayerControlView.VisibilityListener implementation

    override fun onVisibilityChanged(visibility: Int) {
        //debugRootView.setVisibility(visibility);
        if (visibility == View.VISIBLE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) playerView?.hideController()
    }

    // Internal methods

    private fun setContentView() {
        setContentView(binding.root)
    }

    /**
     * @return Whether initialization was successful.
     */
    private fun initializePlayer(): Boolean {
        if (player == null) {
            val intent = intent

            mediaItems = createMediaItems(intent)
            if (mediaItems!!.isEmpty()) {
                return false
            }

            lastSeenTracks = Tracks.EMPTY
            val playerBuilder = ExoPlayer.Builder( /* context= */this)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
            setRenderersFactory(
                playerBuilder, intent.getBooleanExtra(PREFER_EXTENSION_DECODERS_EXTRA, false))
            player = playerBuilder.build()
            player?.trackSelectionParameters = trackSelectionParameters!!
            player?.addListener(PlayerEventListener())
            player?.addAnalyticsListener(EventLogger())
            player?.setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */true)
            player?.playWhenReady = startAutoPlay
            playerView?.player = player
            configurePlayerWithServerSideAdsLoader()
            debugViewHelper = DebugTextViewHelper(player!!, debugTextView!!)
            debugViewHelper?.start()
        }
        val haveStartPosition = startItemIndex != C.INDEX_UNSET
        if (haveStartPosition) {
            player!!.seekTo(startItemIndex, startPosition)
        }
        player?.setMediaItems(mediaItems!!,  /* resetPosition= */ !haveStartPosition)
        player?.prepare()
        updateButtonVisibility()

        if (playerNotificationManager == null) {
            playerNotificationManager = initializeNotification()
        }
        playerNotificationManager?.setPlayer(player)
        return true
    }

    private fun createMediaSourceFactory(): MediaSource.Factory {
        val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
        drmSessionManagerProvider.setDrmHttpDataSourceFactory(
            DemoUtil.getHttpDataSourceFactory( /* context= */this))
        val serverSideAdLoaderBuilder =
            ImaServerSideAdInsertionMediaSource.AdsLoader.Builder( /* context= */this, playerView!!)
        if (serverSideAdsLoaderState != null) {
            serverSideAdLoaderBuilder.setAdsLoaderState(serverSideAdsLoaderState!!)
        }
        serverSideAdsLoader = serverSideAdLoaderBuilder.build()
        val imaServerSideAdInsertionMediaSourceFactory =
            ImaServerSideAdInsertionMediaSource.Factory(
                serverSideAdsLoader!!,
                DefaultMediaSourceFactory(/* context= */ this).setDataSourceFactory(dataSourceFactory!!))
        return DefaultMediaSourceFactory(/* context= */ this)
            .setDataSourceFactory(dataSourceFactory!!)
            .setDrmSessionManagerProvider(drmSessionManagerProvider)
            .setLocalAdInsertionComponents(
                this::getClientSideAdsLoader, /* adViewProvider= */ playerView!!)
            .setServerSideAdInsertionMediaSourceFactory(imaServerSideAdInsertionMediaSourceFactory)
    }

    private fun setRenderersFactory(
        playerBuilder: ExoPlayer.Builder, preferExtensionDecoders: Boolean) {
        val renderersFactory =
            DemoUtil.buildRenderersFactory( /* context= */this, preferExtensionDecoders)
        playerBuilder.setRenderersFactory(renderersFactory)
    }

    private fun configurePlayerWithServerSideAdsLoader() {
        serverSideAdsLoader?.setPlayer(player!!)
    }

    private fun createMediaItems(intent: Intent): List<MediaItem> {
        val action = intent.action
        val actionIsListView = IntentUtil.ACTION_VIEW_LIST == action
        if (!actionIsListView && IntentUtil.ACTION_VIEW != action) {
            showToast(getString(R.string.unexpected_intent_action, action))
            finish()
            return Collections.emptyList()
        }
        val mediaItems: List<MediaItem> =
            createMediaItems(intent, DemoUtil.getDownloadTracker( /* context= */this))
        for (i in mediaItems.indices) {
            val mediaItem = mediaItems[i]

            if (!Util.checkCleartextTrafficPermitted(mediaItem)) {
                showToast(R.string.error_cleartext_not_permitted)
                finish()
                return Collections.emptyList()
            }
            if (Util.maybeRequestReadExternalStoragePermission( /* activity= */this, mediaItem)) {
                // The player will be reinitialized if the permission is granted.
                return Collections.emptyList()
            }

            val drmConfiguration: MediaItem.DrmConfiguration? = mediaItem.localConfiguration?.drmConfiguration
            if (drmConfiguration != null) {
                if (!FrameworkMediaDrm.isCryptoSchemeSupported(drmConfiguration.scheme)) {
                    showToast(R.string.error_drm_unsupported_scheme)
                    finish()
                    return Collections.emptyList()
                }
            }
        }
        return mediaItems
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getClientSideAdsLoader(adsConfiguration: MediaItem.AdsConfiguration): AdsLoader {
        // The ads loader is reused for multiple playbacks, so that ad playback can resume.
        if (clientSideAdsLoader == null) {
            clientSideAdsLoader = ImaAdsLoader.Builder( /* context= */this).build()
        }
        clientSideAdsLoader?.setPlayer(player)
        return clientSideAdsLoader!!
    }

    private fun releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters()
            updateStartPosition()
            releaseServerSideAdsLoader()
            debugViewHelper?.stop()
            debugViewHelper = null
            player?.release()
            player = null
            playerView?.player = null
            mediaItems = Collections.emptyList()
        }
        if (clientSideAdsLoader != null) {
            clientSideAdsLoader?.setPlayer(null)
        } else {
            playerView?.adViewGroup?.removeAllViews()
        }

        releaseNotification()
    }

    private fun releaseServerSideAdsLoader() {
        serverSideAdsLoaderState = serverSideAdsLoader?.release()
        serverSideAdsLoader = null
    }

    private fun releaseClientSideAdsLoader() {
        if (clientSideAdsLoader != null) {
            clientSideAdsLoader?.release()
            clientSideAdsLoader = null
            playerView?.adViewGroup?.removeAllViews()
        }
    }

    private fun saveServerSideAdsLoaderState(outState: Bundle) {
        if (serverSideAdsLoaderState != null) {
            outState.putBundle(KEY_SERVER_SIDE_ADS_LOADER_STATE, serverSideAdsLoaderState?.toBundle())
        }
    }

    private fun restoreServerSideAdsLoaderState(savedInstanceState: Bundle) {
        val adsLoaderStateBundle = savedInstanceState.getBundle(KEY_SERVER_SIDE_ADS_LOADER_STATE)
        if (adsLoaderStateBundle != null) {
            serverSideAdsLoaderState =
                ImaServerSideAdInsertionMediaSource.AdsLoader.State.CREATOR.fromBundle(
                    adsLoaderStateBundle)
        }
    }

    private fun updateTrackSelectorParameters() {
        if (player != null) {
            trackSelectionParameters = player?.trackSelectionParameters
        }
    }

    private fun updateStartPosition() {
        if (player != null) {
            startAutoPlay = player!!.playWhenReady
            startItemIndex = player!!.currentMediaItemIndex
            startPosition = max(0, player!!.contentPosition)
        }
    }

    private fun clearStartPosition() {
        startAutoPlay = true
        startItemIndex = C.INDEX_UNSET
        startPosition = C.TIME_UNSET
    }

    // User controls

    private fun updateButtonVisibility() {
        selectTracksButton?.isVisible = player != null && TrackSelectionDialog.willHaveContent(player)
    }

    private fun showControls() {
        //debugRootView.setVisibility(View.VISIBLE)
    }

    private fun showToast(messageId: Int) {
        showToast(getString(messageId))
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private inner class PlayerEventListener : Player.Listener {

        override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                showControls()
            }
            updateButtonVisibility()
        }

        override fun onPlayerError(@NonNull error: PlaybackException) {
            if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                player?.seekToDefaultPosition()
                player?.prepare()
            } else {
                updateButtonVisibility()
                showControls()
            }
        }

        override fun onTracksChanged(tracks: Tracks) {
            updateButtonVisibility()
            if (tracks == lastSeenTracks) {
                return
            }
            if (tracks.containsType(C.TRACK_TYPE_VIDEO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_VIDEO,  /* allowExceedsCapabilities= */true)) {
                showToast(R.string.error_unsupported_video)
            }
            if (tracks.containsType(C.TRACK_TYPE_AUDIO)
                && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO,  /* allowExceedsCapabilities= */true)) {
                showToast(R.string.error_unsupported_audio)
            }
            lastSeenTracks = tracks
        }

        /* END */

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (player?.isCurrentMediaItemLive == true && player?.isCurrentMediaItemDynamic == true) {
                liveIndicator.showView()
                if (player?.currentLiveOffset!! >= 10000) {
                    liveIndicator.text = resources.getString(R.string.go_live_caps)
                }
            } else {
                liveIndicator.hideView()
            }
            exoPosition.isVisible = !player?.isCurrentMediaItemDynamic!!
            exoDuration.isVisible = !player?.isCurrentMediaItemDynamic!!

            if (player?.mediaItemCount!! > 1) {
                exoNext.isEnabled = player?.hasNextMediaItem() == true
                exoPrevious.showView()
                exoNext.showView()
            } else {
                exoPrevious.hideView()
                exoNext.hideView()
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            super.onMediaMetadataChanged(mediaMetadata)
            val mediaTitle = mediaMetadata.title
            if (mediaTitle != null && mediaTitle != "null")
                exoTitle.text = mediaTitle
        }
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<PlaybackException> {

        override fun getErrorMessage(e: PlaybackException): Pair<Int, String> {
            var errorString: String = getString(R.string.error_generic)
            val cause = e.cause
            if (cause is DecoderInitializationException) {
                // Special case for decoder initialization failures.
                errorString = if (cause.codecInfo == null) {
                    if (cause.cause is DecoderQueryException) {
                        getString(R.string.error_querying_decoders)
                    } else if (cause.secureDecoderRequired) {
                        getString(R.string.error_no_secure_decoder, cause.mimeType)
                    } else {
                        getString(R.string.error_no_decoder, cause.mimeType)
                    }
                } else {
                    getString(R.string.error_instantiating_decoder, cause.codecInfo!!.name)
                }
            }
            return Pair.create(0, errorString)
        }
    }

    private fun createMediaItems(intent: Intent, downloadTracker: DownloadTracker) : List<MediaItem> {
        val mediaItems: MutableList<MediaItem> = ArrayList()
        for (item in IntentUtil.createMediaItemsFromIntent(intent)) {
            mediaItems.add(maybeSetDownloadProperties(item, downloadTracker.getDownloadRequest(item.localConfiguration?.uri)))
        }
        return mediaItems
    }

    private fun maybeSetDownloadProperties(item: MediaItem, @Nullable downloadRequest: DownloadRequest?) : MediaItem {
        if (downloadRequest == null) {
            return item
        }
        val builder = item.buildUpon()
        builder
            .setMediaId(downloadRequest.id)
            .setUri(downloadRequest.uri)
            .setCustomCacheKey(downloadRequest.customCacheKey)
            .setMimeType(downloadRequest.mimeType)
            .setStreamKeys(downloadRequest.streamKeys)
        @Nullable val drmConfiguration: MediaItem.DrmConfiguration? = item.localConfiguration?.drmConfiguration
        if (drmConfiguration != null) {
            builder.setDrmConfiguration(
                drmConfiguration.buildUpon().setKeySetId(downloadRequest.keySetId).build())
        }
        return builder.build()
    }

    /* End */

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerView?.hideController()
            if (!player?.isPlaying!!) player?.play()
            if (playerLocked) exoUnlock.hideView()

            broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    if (p1 != null && p1.action == ACTION_PIP_MEDIA_CONTROL && player != null) {
                        if (player!!.isPlaying) {
                            player!!.pause()
                        } else {
                            player!!.play()
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) updatePictureInPictureParams()
                    }
                }
            }
            registerReceiver(broadcastReceiver, IntentFilter(ACTION_PIP_MEDIA_CONTROL))
        } else {
            unregisterReceiver(broadcastReceiver)
            broadcastReceiver = null
            if (player != null && !player!!.isPlaying && !playerLocked) playerView?.showController()
            if (playerLocked) exoUnlock.showView()
        }
        binding.appName.isVisible = !isInPictureInPictureMode
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureParams() : PictureInPictureParams {
        val paramsBuilder: PictureInPictureParams.Builder = PictureInPictureParams.Builder()
        val actions: ArrayList<RemoteAction> = ArrayList()
        val aspectRatio = Rational(playerView!!.width, playerView!!.height)
        val sourceRectHint = Rect()
        playerView?.getGlobalVisibleRect(sourceRectHint)

        val actionIntent = Intent(ACTION_PIP_MEDIA_CONTROL)
        val pendingIntent =
            PendingIntent.getBroadcast(context, PIP_REQUEST_CODE, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        PIP_ACTION_ICON_ID = if (player?.isPlaying == true) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
        val icon: Icon = Icon.createWithResource(context, PIP_ACTION_ICON_ID)

        actions.add(RemoteAction(
            icon,
            "Play/Pause",
            "Play/Pause Video",
            pendingIntent
        ))

        paramsBuilder.setAspectRatio(aspectRatio)
            .setSourceRectHint(sourceRectHint)
            .setActions(actions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) paramsBuilder.setAutoEnterEnabled(true)
        val pictureInPictureParams = paramsBuilder.build()
        setPictureInPictureParams(pictureInPictureParams)
        return pictureInPictureParams
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val appOPS = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val pipStatus = appOPS.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
            if (pipStatus) {
                this.enterPictureInPictureMode(updatePictureInPictureParams())
            } else {
                val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                controlWindowInsets(true)
            } else {
                controlSystemUI(true)
            }
        }
    }

    @Deprecated("Deprecated in Api level 30")
    private fun controlSystemUI(hide: Boolean) {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "default immersive", replace it with SYSTEM_UI_FLAG_IMMERSIVE
        val hideBehavior = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Shows the system bars by removing all the flags
        // except for the ones that make the content appear under the system bars.
        val showBehavior = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        if (hide) {
            window.decorView.systemUiVisibility = hideBehavior
        } else {
            window.decorView.systemUiVisibility = showBehavior
        }
    }

    @Suppress("SameParameterValue")
    @RequiresApi(Build.VERSION_CODES.R)
    private fun controlWindowInsets(hide: Boolean) {
        // WindowInsetsController can hide or show specified system bars.
        val insetsController = window.decorView.windowInsetsController ?: return
        // The behaviour of the immersive mode.
        val behavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // The type of system bars to hide or show.
        val type = WindowInsets.Type.systemBars()
        insetsController.systemBarsBehavior = behavior
        if (hide) {
            insetsController.hide(type)
        } else {
            insetsController.show(type)
        }
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun openSettings() {
        val playerMenuBottomSheet = PlayerMenuBottomSheet()
        playerMenuBottomSheet.videoTrackClickListener = OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.selectVideoTrack(context, player)
        }
        playerMenuBottomSheet.audioTrackClickListener = OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.selectAudioTrack(context, player)
        }
        playerMenuBottomSheet.subTrackClickListener = OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.selectSubTrack(context, player)
        }
        playerMenuBottomSheet.settingsClickListener = OnClickListener {
            playerMenuBottomSheet.dismiss()
            if (!isShowingTrackSelectionDialog && TrackSelectionDialog.willHaveContent(player)) {
                isShowingTrackSelectionDialog = true
                val trackSelectionDialog = TrackSelectionDialog.createForPlayer(player) /* onDismissListener= */ {
                    isShowingTrackSelectionDialog = false
                }
                trackSelectionDialog.show(supportFragmentManager,  /* tag= */null)
            }
        }
        playerMenuBottomSheet.playbackSpeedClickListener = OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.setPlaybackSpeed(context, player)
        }
        playerMenuBottomSheet.show(supportFragmentManager, PlayerMenuBottomSheet.TAG)
    }

    private fun lockPlayer() {
        if (playerView != null) {
            playerView!!.hideController()
            playerView!!.useController = false
            exoUnlock.showView()
            playerLocked = true
        }
    }

    private fun unlockPlayer() {
        if (playerView != null) {
            exoUnlock.hideView()
            playerView!!.useController = true
            playerView!!.showController()
            playerLocked = false
        }
    }

    /* Experimental */

    override fun onDown(p0: MotionEvent): Boolean {
        minSwipeY = 0f
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
        return
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(event1: MotionEvent, event2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (playerLocked) return false

        minSwipeY += distanceY

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val border = 20 * Resources.getSystem().displayMetrics.density.toInt()

        if( event1.x < border || event1.y < border || event1.x > screenWidth - border || event1.y > screenHeight - border)
            return false

        if(abs(distanceX) < abs(distanceY) && abs(minSwipeY) > 50){
            if(event1.x < screenWidth/2){
                //brightness
                binding.brightnessControl.showView()
                binding.volumeControl.hideView()
                val increase = distanceY > 0
                val newValue = if(increase) brightness + 1 else brightness - 1
                if(newValue in 0..30) brightness = newValue
                setScreenBrightness(brightness)
            }
            else {
                //volume
                val volumeIconSrc: Int
                binding.brightnessControl.hideView()
                binding.volumeControl.showView()
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                volume = currentVolume
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val increase = distanceY > 0
                val newValue: Int
                if(increase) {
                    newValue = volume + 1
                    volumeIconSrc = R.drawable.ic_baseline_volume_up_24
                } else {
                    newValue = volume - 1
                    volumeIconSrc = if (newValue > 0) {
                        R.drawable.ic_baseline_volume_down_24
                    } else {
                        R.drawable.ic_baseline_volume_mute_24
                    }
                }
                if(newValue in 0..maxVolume) volume = newValue
                binding.volumeIcon.setImageResource(volumeIconSrc)
                binding.volumeProgress.progress = (volume * 100) / maxVolume
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
            }
            minSwipeY = 0f
        }
        return true
    }

    override fun onLongPress(p0: MotionEvent) {
        return
    }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeSwipeControl() {
        playerView?.setOnTouchListener { _, motionEvent ->
            if (playerLocked) return@setOnTouchListener false

            gestureDetectorCompat.onTouchEvent(motionEvent)
            if(motionEvent.action == MotionEvent.ACTION_UP) {
                binding.brightnessControl.hideView()
                binding.volumeControl.hideView()
            }
            return@setOnTouchListener false
        }
    }

    private fun setScreenBrightness(value: Int) {
        val brightnessIconSrc: Int
        val brightnessConstant = 1.0f/30
        val layoutParams = this.window.attributes
        layoutParams.screenBrightness = brightnessConstant * value
        val brightnessPercentage = (value * 100) / 30
        brightnessIconSrc = when (brightnessPercentage) {
            0 -> {
                R.drawable.ic_baseline_brightness_low_24
            }
            100 -> {
                R.drawable.ic_baseline_brightness_high_24
            }
            else -> {
                R.drawable.ic_baseline_brightness_medium_24
            }
        }
        binding.brightnessIcon.setImageResource(brightnessIconSrc)
        binding.brightnessProgress.progress = brightnessPercentage
        this.window.attributes = layoutParams
    }

    private fun initializeNotification(): PlayerNotificationManager {
        val channelId = createNotificationChannel()
        val notificationId = 101
        val mediaDescriptionAdapter = DescriptionAdapter(context)

        val playerNotificationManager = PlayerNotificationManager
            .Builder(context, notificationId, channelId)
            .setMediaDescriptionAdapter(mediaDescriptionAdapter)
            .build()

        playerNotificationManager.setUsePreviousAction(false)
        playerNotificationManager.setUseNextAction(false)
        playerNotificationManager.setUseStopAction(true)
        playerNotificationManager.setSmallIcon(R.drawable.ic_notification)

        return playerNotificationManager
    }

    private fun releaseNotification() {
        if (playerNotificationManager != null) {
            playerNotificationManager?.setPlayer(null)
            playerNotificationManager = null
        }
    }

    private fun createNotificationChannel() : String {
        val channelId: String = getString(R.string.player_notification_channel_id)
        val channelName: String = getString(R.string.player_notification_channel_name)
        val channelDescription = getString(R.string.player_notification_channel_description)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtil(context).createNotificationChannel(channelName, channelDescription, channelId, NotificationManager.IMPORTANCE_LOW)
        }
        return channelId
    }
}