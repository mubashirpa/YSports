// Last updated on 11 May 2022
// Latest commit on Feb 16

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
import android.util.Log
import android.util.Pair
import android.util.Rational
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegLibrary
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader
import com.google.android.exoplayer2.ext.ima.ImaServerSideAdInsertionMediaSource
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException
import com.google.android.exoplayer2.offline.DownloadRequest
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ads.AdsLoader
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.util.DebugTextViewHelper
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.EventLogger
import com.google.android.exoplayer2.util.Util
import org.checkerframework.checker.nullness.qual.MonotonicNonNull
import ysports.app.databinding.ActivityPlayerBinding
import ysports.app.fragments.PlayerMenuBottomSheet
import ysports.app.player.*
import ysports.app.player.IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA
import java.util.*
import kotlin.math.abs
import kotlin.math.max

@Suppress("PrivatePropertyName")
class PlayerActivity : AppCompatActivity(), View.OnClickListener, StyledPlayerControlView.VisibilityListener,
    GestureDetector.OnGestureListener {

    // Saved instance state keys.

    private val KEY_TRACK_SELECTION_PARAMETERS: String = "track_selection_parameters"
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
    private var trackSelector: DefaultTrackSelector? = null
    private var trackSelectionParameters: DefaultTrackSelector.Parameters? = null
    private var debugViewHelper: DebugTextViewHelper? = null
    private var lastSeenTracksInfo: TracksInfo? = null
    private var startAutoPlay = false
    private var startItemIndex = 0
    private var startPosition: Long = 0

    // For ad playback only.

    @Nullable private var clientSideAdsLoader: AdsLoader? = null
    @Nullable private var serverSideAdsLoader: ImaServerSideAdInsertionMediaSource.AdsLoader? = null
    private var serverSideAdsLoaderState: @MonotonicNonNull ImaServerSideAdInsertionMediaSource.AdsLoader.State? = null

    /* END */

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var context: Context
    private val TAG: String = "PlayerActivity"

    private lateinit var exoDuration: TextView
    private lateinit var navigationButton: ImageButton
    private lateinit var exoPIP: ImageButton
    private lateinit var changeAspectRatioButton: ImageButton
    private lateinit var liveIndicator: TextView
    private lateinit var gestureDetectorCompat: GestureDetectorCompat
    private var minSwipeY: Float = 0f
    private var brightness: Int = 0
    private var volume: Int = 0
    private lateinit var audioManager: AudioManager
    private val ACTION_PIP_MEDIA_CONTROL = "pip_media_control"
    private val PIP_REQUEST_CODE = 101
    private var PIP_ACTION_ICON_ID = R.drawable.ic_baseline_play_arrow_24
    private var broadcastReceiver: BroadcastReceiver? = null
    private val playerUtil = PlayerUtil()
    private lateinit var exoLock: ImageButton
    private lateinit var exoUnlock: ImageButton
    private var playerLocked = false

    // Activity lifecycle.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        context = this
        dataSourceFactory = DemoUtil.getDataSourceFactory(context)
        setContentView(binding.root)

        debugTextView = binding.debugTextView
        selectTracksButton = findViewById(R.id.exo_settings)
        selectTracksButton!!.setOnClickListener(this)

        exoDuration = findViewById(com.google.android.exoplayer2.ui.R.id.exo_duration)
        navigationButton = findViewById(R.id.exo_navigation)
        exoPIP = findViewById(R.id.exo_pip)
        changeAspectRatioButton = findViewById(R.id.exo_change_aspect_ratio)
        liveIndicator = findViewById(R.id.exo_live_indicator)
        navigationButton.setOnClickListener(this)
        exoPIP.setOnClickListener(this)
        changeAspectRatioButton.setOnClickListener(this)
        liveIndicator.setOnClickListener(this)
        exoPIP.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        gestureDetectorCompat = GestureDetectorCompat(context, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        exoLock = findViewById(R.id.exo_lock)
        exoUnlock = findViewById(R.id.exo_unlock)
        exoLock.setOnClickListener(this)
        exoUnlock.setOnClickListener(this)

        playerView = binding.playerView
        playerView!!.setControllerVisibilityListener(this)
        playerView!!.setErrorMessageProvider(PlayerErrorMessageProvider())
        playerView!!.requestFocus()

        if (savedInstanceState != null) {
            // Restore as DefaultTrackSelector.Parameters in case ExoPlayer specific parameters were set.
            trackSelectionParameters = DefaultTrackSelector.Parameters.CREATOR.fromBundle(
                savedInstanceState.getBundle(KEY_TRACK_SELECTION_PARAMETERS)!!)
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY)
            startItemIndex = savedInstanceState.getInt(KEY_ITEM_INDEX)
            startPosition = savedInstanceState.getLong(KEY_POSITION)
            val adsLoaderStateBundle = savedInstanceState.getBundle(KEY_SERVER_SIDE_ADS_LOADER_STATE)
            if (adsLoaderStateBundle != null) {
                serverSideAdsLoaderState =
                    ImaServerSideAdInsertionMediaSource.AdsLoader.State.CREATOR.fromBundle(
                        adsLoaderStateBundle)
            }
        } else {
            trackSelectionParameters = ParametersBuilder( /* context= */context).build()
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

    override fun onBackPressed() {
        if (playerLocked) showToast("Player is locked") else {
            super.onBackPressed()
            finish()
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
        outState.putBundle(KEY_TRACK_SELECTION_PARAMETERS, trackSelectionParameters!!.toBundle())
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay)
        outState.putInt(KEY_ITEM_INDEX, startItemIndex)
        outState.putLong(KEY_POSITION, startPosition)
        if (serverSideAdsLoaderState != null) {
            outState.putBundle(KEY_SERVER_SIDE_ADS_LOADER_STATE, serverSideAdsLoaderState!!.toBundle())
        }
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
            navigationButton -> onBackPressed()
            exoPIP -> enterPictureInPicture()
            changeAspectRatioButton -> {
                if (playerView!!.resizeMode != AspectRatioFrameLayout.RESIZE_MODE_FILL) {
                    playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                } else {
                    playerView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
            liveIndicator -> player?.seekToDefaultPosition()
            exoLock -> lockPlayer()
            exoUnlock -> unlockPlayer()
        }
    }

    // StyledPlayerControlView.VisibilityListener implementation

    override fun onVisibilityChange(visibility: Int) {
        //debugRootView.setVisibility(visibility);
        if (visibility == View.VISIBLE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode) playerView?.hideController()
    }

    // Internal methods
    // setContentView is already set

    /** @return Whether initialization was successful. */
    private fun initializePlayer(): Boolean {
        if (player == null) {
            val intent = intent

            mediaItems = createMediaItems(intent)
            if (mediaItems!!.isEmpty()) {
                return false
            }

            Log.d(TAG, "FfmpegLibrary:${FfmpegLibrary.isAvailable()}")

            val preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS_EXTRA, false)
            val renderersFactory = DemoUtil.buildRenderersFactory(context, preferExtensionDecoders)

            trackSelector = DefaultTrackSelector(context)
            lastSeenTracksInfo = TracksInfo.EMPTY
            player = ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .setMediaSourceFactory(createMediaSourceFactory())
                .setTrackSelector(trackSelector!!)
                .build()
            player!!.trackSelectionParameters = trackSelectionParameters!!
            player!!.addListener(PlayerEventListener())
            player!!.addAnalyticsListener(EventLogger(trackSelector))


            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build()

            player!!.setAudioAttributes(audioAttributes, true)
            player!!.playWhenReady = startAutoPlay
            playerView!!.player = player
            serverSideAdsLoader?.setPlayer(player!!)
            debugViewHelper = DebugTextViewHelper(player!!, debugTextView!!)
            debugViewHelper!!.start()
        }
        val haveStartPosition = startItemIndex != C.INDEX_UNSET
        if (haveStartPosition) {
            player!!.seekTo(startItemIndex, startPosition)
        }
        player!!.setMediaItems(mediaItems!!,  /* resetPosition= */!haveStartPosition)
        player!!.prepare()
        updateButtonVisibility()
        return true
    }

    private fun createMediaSourceFactory(): MediaSource.Factory {
        val serverSideAdLoaderBuilder =
            ImaServerSideAdInsertionMediaSource.AdsLoader.Builder( /* context= */context, playerView!!)
        if (serverSideAdsLoaderState != null) {
            serverSideAdLoaderBuilder.setAdsLoaderState(serverSideAdsLoaderState!!)
        }
        serverSideAdsLoader = serverSideAdLoaderBuilder.build()
        val imaServerSideAdInsertionMediaSourceFactory =
            ImaServerSideAdInsertionMediaSource.Factory(
                serverSideAdsLoader!!, DefaultMediaSourceFactory(dataSourceFactory!!))
        return DefaultMediaSourceFactory(dataSourceFactory!!)
            .setAdsLoaderProvider(this::getClientSideAdsLoader)
            .setAdViewProvider(playerView)
            .setServerSideAdInsertionMediaSourceFactory(imaServerSideAdInsertionMediaSourceFactory)
    }

    private fun createMediaItems(intent: Intent): List<MediaItem> {
        val action = intent.action
        val actionIsListView = IntentUtil.ACTION_VIEW_LIST == action
        if (!actionIsListView && IntentUtil.ACTION_VIEW != action) {
            showToast(getString(R.string.unexpected_intent_action, action))
            finish()
            return Collections.emptyList()
        }

        val mediaItems = createMediaItems(intent, DemoUtil.getDownloadTracker( /* context= */this))
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
            val drmConfiguration: MediaItem.DrmConfiguration? =
                mediaItem.localConfiguration!!.drmConfiguration
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
    private fun getClientSideAdsLoader(adsConfiguration: MediaItem.AdsConfiguration): AdsLoader? {
        // The ads loader is reused for multiple playbacks, so that ad playback can resume.
        if (clientSideAdsLoader == null) {
            clientSideAdsLoader = ImaAdsLoader.Builder( /* context= */context).build()
        }
        clientSideAdsLoader!!.setPlayer(player)
        return clientSideAdsLoader
    }

    private fun releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters()
            updateStartPosition()
            serverSideAdsLoaderState = serverSideAdsLoader!!.release()
            serverSideAdsLoader = null
            debugViewHelper!!.stop()
            debugViewHelper = null
            player!!.release()
            player = null
            playerView?.player = null
            mediaItems = Collections.emptyList()
        }
        if (clientSideAdsLoader != null) {
            clientSideAdsLoader!!.setPlayer(null)
        } else {
            playerView!!.adViewGroup.removeAllViews()
        }
    }

    private fun releaseClientSideAdsLoader() {
        if (clientSideAdsLoader != null) {
            clientSideAdsLoader!!.release()
            clientSideAdsLoader = null
            playerView!!.adViewGroup.removeAllViews()
        }
    }

    private fun updateTrackSelectorParameters() {
        if (player != null) {
            // Until the app is fully migrated to TrackSelectionParameters, rely on ExoPlayer to use
            // DefaultTrackSelector by default.
            trackSelectionParameters =
                player!!.trackSelectionParameters as DefaultTrackSelector.Parameters
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
        selectTracksButton!!.isVisible =
            player != null && TrackSelectionDialog.willHaveContent(trackSelector!!)
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

        override fun onTracksInfoChanged(tracksInfo: TracksInfo) {
            updateButtonVisibility()
            if (tracksInfo === lastSeenTracksInfo) {
                return
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(
                    C.TRACK_TYPE_VIDEO, /* allowExceedsCapabilities= */ true)) {
                showToast(R.string.error_unsupported_video)
            }
            if (!tracksInfo.isTypeSupportedOrEmpty(
                    C.TRACK_TYPE_AUDIO, /* allowExceedsCapabilities= */ true)) {
                showToast(R.string.error_unsupported_audio)
            }
            lastSeenTracksInfo = tracksInfo
        }

        /* Extra */

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (player != null && player!!.isCurrentMediaItemLive && player!!.isCurrentMediaItemDynamic) {
                liveIndicator.showView()
            } else {
                liveIndicator.hideView()
            }
            exoDuration.isVisible = !player?.isCurrentMediaItemDynamic!!
        }
    }

    private inner class PlayerErrorMessageProvider : ErrorMessageProvider<PlaybackException> {

        override fun getErrorMessage(e: PlaybackException): Pair<Int, String> {
            var errorString: String? = getString(R.string.error_generic)
            val cause = e.cause
            if (cause is DecoderInitializationException) {
                // Special case for decoder initialization failures.
                errorString = if (cause.codecInfo == null) {
                    when {
                        cause.cause is DecoderQueryException -> {
                            getString(R.string.error_querying_decoders)
                        }
                        cause.secureDecoderRequired -> {
                            getString(R.string.error_no_secure_decoder, cause.mimeType)
                        }
                        else -> {
                            getString(R.string.error_no_decoder, cause.mimeType)
                        }
                    }
                } else {
                    getString(R.string.error_instantiating_decoder, cause.codecInfo!!.name)
                }
            }
            return Pair.create(0, errorString)
        }
    }

    private fun createMediaItems(intent: Intent, downloadTracker: DownloadTracker): List<MediaItem> {
        val mediaItems: MutableList<MediaItem> = ArrayList()
        for (item in IntentUtil.createMediaItemsFromIntent(intent)) {
            val downloadRequest: DownloadRequest? = downloadTracker.getDownloadRequest(item.localConfiguration!!.uri)
            if (downloadRequest != null) {
                val builder = item.buildUpon()
                builder
                    .setMediaId(downloadRequest.id)
                    .setUri(downloadRequest.uri)
                    .setCustomCacheKey(downloadRequest.customCacheKey)
                    .setMimeType(downloadRequest.mimeType)
                    .setStreamKeys(downloadRequest.streamKeys)
                val drmConfiguration: MediaItem.DrmConfiguration? = item.localConfiguration!!.drmConfiguration
                if (drmConfiguration != null) {
                    builder.setDrmConfiguration(
                        drmConfiguration.buildUpon().setKeySetId(downloadRequest.keySetId).build()
                    )
                }
                mediaItems.add(builder.build())
            } else {
                mediaItems.add(item)
            }
        }
        return mediaItems
    }

    /* End */

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerView?.hideController()
            if (!player?.isPlaying!!) player?.play()

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
            if (player != null && !player!!.isPlaying) playerView?.showController()
        }
        binding.appName.isVisible = !isInPictureInPictureMode
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureParams() : PictureInPictureParams {
        val paramsBuilder: PictureInPictureParams.Builder = PictureInPictureParams.Builder()
        val actions: ArrayList<RemoteAction> = ArrayList()
        val aspectRatio = Rational(playerView!!.width, playerView!!.height)
        val sourceRectHint = Rect()
        playerView?.getGlobalVisibleRect(sourceRectHint)

        val actionIntent = Intent(ACTION_PIP_MEDIA_CONTROL)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(context, PIP_REQUEST_CODE, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getBroadcast(context, PIP_REQUEST_CODE, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
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
        playerMenuBottomSheet.videoTrackClickListener = View.OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.selectVideoTrack(context, lastSeenTracksInfo!!, trackSelector)
        }
        playerMenuBottomSheet.audioTrackClickListener = View.OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.selectAudioTrack(context, lastSeenTracksInfo, trackSelector)
        }
        playerMenuBottomSheet.subTrackClickListener = View.OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.selectSubTrack(context, lastSeenTracksInfo, trackSelector)
        }
        playerMenuBottomSheet.settingsClickListener = View.OnClickListener {
            playerMenuBottomSheet.dismiss()
            if (!isShowingTrackSelectionDialog && TrackSelectionDialog.willHaveContent(trackSelector!!)) {
                isShowingTrackSelectionDialog = true
                val trackSelectionDialog = TrackSelectionDialog.createForTrackSelector(trackSelector!!) {
                    // onDismissListener
                    isShowingTrackSelectionDialog = false
                }
                trackSelectionDialog.show(supportFragmentManager, null)
            }
        }
        playerMenuBottomSheet.playbackSpeedClickListener = View.OnClickListener {
            playerMenuBottomSheet.dismiss()
            playerUtil.setPlaybackSpeed(context, player)
        }
        playerMenuBottomSheet.show(supportFragmentManager, PlayerMenuBottomSheet.TAG)
    }

    /* Experimental */

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

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeSwipeControl() {
        playerView?.setOnTouchListener { view, motionEvent ->
            //playerView.isDoubleTapEnabled = false
            //playerView.isDoubleTapEnabled = true
            gestureDetectorCompat.onTouchEvent(motionEvent)
            if(motionEvent.action == MotionEvent.ACTION_UP) {
                binding.brightnessIcon.hideView()
                binding.volumeIcon.hideView()
                //for immersive mode
                WindowCompat.setDecorFitsSystemWindows(window, false)
                WindowInsetsControllerCompat(window, view).let { controller ->
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
            return@setOnTouchListener false
        }
    }

    override fun onDown(p0: MotionEvent?): Boolean {
        minSwipeY = 0f
        return false
    }

    override fun onShowPress(p0: MotionEvent?) = Unit

    override fun onSingleTapUp(p0: MotionEvent?): Boolean = false

    override fun onScroll(event1: MotionEvent?, event2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        minSwipeY += distanceY

        val screenWidth = Resources.getSystem().displayMetrics.widthPixels
        val screenHeight = Resources.getSystem().displayMetrics.heightPixels
        val border = 100 * Resources.getSystem().displayMetrics.density.toInt()

        if (event1 != null) {
            if( event1.x < border || event1.y < border || event1.x > screenWidth - border || event1.y > screenHeight - border)
                return false

            if(abs(distanceX) < abs(distanceY) && abs(minSwipeY) > 50){
                if(event1.x < screenWidth/2){
                    //brightness
                    binding.brightnessIcon.showView()
                    binding.volumeIcon.hideView()
                    val increase = distanceY > 0
                    val newValue = if(increase) brightness + 1 else brightness - 1
                    if(newValue in 0..30) brightness = newValue
                    binding.brightnessIcon.text = brightness.toString()
                    setScreenBrightness(brightness)
                }
                else {
                    //volume
                    binding.brightnessIcon.hideView()
                    binding.volumeIcon.showView()
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    volume = currentVolume
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val increase = distanceY > 0
                    val newValue = if(increase) volume + 1 else volume - 1
                    if(newValue in 0..maxVolume) volume = newValue
                    binding.volumeIcon.text = volume.toString()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                }
                minSwipeY = 0f
            }
        }
        return true
    }

    override fun onLongPress(p0: MotionEvent?) = Unit

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean = false

    private fun setScreenBrightness(value: Int) {
        val d = 1.0f/30
        val lp = this.window.attributes
        lp.screenBrightness = d * value
        this.window.attributes = lp
    }
}