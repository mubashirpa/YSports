package ysports.app

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView
import ysports.app.databinding.ActivityYoutubePlayerBinding

@Suppress("PrivatePropertyName")
class YouTubePlayerActivity : YouTubeBaseActivity() {

    private lateinit var binding: ActivityYoutubePlayerBinding
    private lateinit var context: Context
    private lateinit var youTubePlayer: YouTubePlayerView
    private lateinit var API_KEY: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        youTubePlayer = binding.youtubePlayer
        API_KEY = BuildConfig.youtube_api
        var videoID = intent.getStringExtra("VIDEO_URL") ?: ""

        if (videoID.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_empty_url), Toast.LENGTH_LONG).show()
            finish()
        }
        if (videoID.contains("https://youtu.be/")) {
            videoID = videoID.replace("https://youtu.be/", "")
        }

        youTubePlayer.initialize(API_KEY, object : YouTubePlayer.OnInitializedListener {
            override fun onInitializationSuccess(provider: YouTubePlayer.Provider, youTubePlayer: YouTubePlayer, wasRestored: Boolean) {
                if (!wasRestored) {
                    youTubePlayer.setShowFullscreenButton(false)
                    youTubePlayer.setFullscreen(true)
                    youTubePlayer.loadVideo(videoID)
                    youTubePlayer.play()
                }
            }

            override fun onInitializationFailure(provider: YouTubePlayer.Provider, youTubeInitializationResult: YouTubeInitializationResult) {
                Toast.makeText(context, getString(R.string.error_initialize_youtube_player), Toast.LENGTH_SHORT).show()
            }
        })
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
}