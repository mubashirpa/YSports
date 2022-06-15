package ysports.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import android.webkit.WebView.RENDERER_PRIORITY_BOUND
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.webkit.*
import com.google.android.material.progressindicator.CircularProgressIndicator
import ysports.app.databinding.ActivityWebBinding
import ysports.app.player.PlayerUtil
import ysports.app.util.AdBlocker
import ysports.app.util.YouTubePlay
import java.net.URISyntaxException
import java.net.URLDecoder

@Suppress("PrivatePropertyName")
class WebActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebBinding
    private lateinit var context: Context
    private lateinit var webView: WebView
    private var WEB_URL: String = ""
    private lateinit var webViewProgressIndicator: CircularProgressIndicator
    private var webViewErrorOccurred: Boolean = false
    private val TAG: String = "WebActivity"
    private val CHROME_SCHEME = "chrome:"
    private val VIDEO_SCHEME = "video:"
    private val INTENT_SCHEME = "intent:"
    private val TORRENT_SCHEME = "magnet:"
    private lateinit var errorView: View
    private lateinit var errorTextView: TextView
    private lateinit var retryButton: Button
    private var handler: Handler? = null
    private val playerUtil = PlayerUtil()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        webView = binding.webView
        webViewProgressIndicator = binding.progressIndicatorWebView
        errorView = findViewById(R.id.error_view)
        errorTextView = binding.errorView.stateDescription
        retryButton = binding.errorView.buttonRetry
        WEB_URL = intent.getStringExtra("WEB_URL") ?: "https://appassets.androidplatform.net/assets/web/error_404/index.html"

        webView.setBackgroundColor(Color.TRANSPARENT)
        AdBlocker.init(context)

        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("ysports.app")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
            .build()

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    WebSettingsCompat.setForceDark(
                        webView.settings,
                        WebSettingsCompat.FORCE_DARK_ON
                    )
                }
                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    WebSettingsCompat.setForceDark(
                        webView.settings,
                        WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            WebSettingsCompat.setForceDarkStrategy(
                webView.settings,
                WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
            )
        }

        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(false)
        }

        // WebView settings
        webView.settings.apply {

            // 'setter for allowUniversalAccessFromFileURLs: Boolean' is deprecated. Deprecated in Java
            allowUniversalAccessFromFileURLs = true

            allowContentAccess = true
            allowFileAccess = true
            blockNetworkImage = false
            blockNetworkLoads = false
            builtInZoomControls = true
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            displayZoomControls = false
            domStorageEnabled = true
            setGeolocationEnabled(true)
            javaScriptCanOpenWindowsAutomatically = true
            javaScriptEnabled = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
            loadWithOverviewMode = true
            loadsImagesAutomatically = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            setNeedInitialFocus(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                offscreenPreRaster = true
            }
            setSupportMultipleWindows(true)
            setSupportZoom(false)
            useWideViewPort = true
            userAgentString = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.61 Mobile Safari/537.36"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.isSoundEffectsEnabled = true
        webView.isLongClickable = true
        webView.requestFocusFromTouch()

        // Renderer Importance API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, true)
        }
        webView.addJavascriptInterface(WebAppInterface(context), "Android")
        webView.webViewClient = CustomWebViewClient(assetLoader)
        webView.webChromeClient = CustomWebChromeClient()
        webView.loadUrl(WEB_URL)

        webView.setOnLongClickListener {
            true
        }

        retryButton.setOnClickListener {
            errorView.hideView()
            webViewProgressIndicator.showView()
            // Using timer to avoid multiple clicking on retry
            handler = Handler(Looper.getMainLooper())
            handler!!.postDelayed({
                if (!isDestroyed) {
                    webView.showView()
                    webView.reload()
                }
            }, 400)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
        webView.destroy()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    @Suppress("unused")
    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun exitActivity() {
            finish()
        }

        @JavascriptInterface
        fun showToast(message: String?) {
            if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun loadPlayer(url: String?) {
            if (url != null) {
                playerUtil.loadPlayer(context, Uri.parse(url), true)
            }
        }

        @JavascriptInterface
        fun loadPlayerYT(url: String?) {
            if (url != null) {
                YouTubePlay(context).playVideo(url)
            }
        }
    }

    inner class CustomWebViewClient(private val assetLoader: WebViewAssetLoader) : WebViewClientCompat() {

        @RequiresApi(26)
        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            if (detail != null && !detail.didCrash()) {
                Log.e(TAG, "System killed the WebView rendering process to reclaim memory. Recreating...")

                webView.also { web ->
                    web.destroy()
                }
                return true
            }

            Log.e(TAG, "The WebView rendering process crashed!")
            return false
        }

        @Nullable
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request.url)
        }

        @RequiresApi(23)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS)) return false
            return overrideUrlLoading(request.url.toString())
        }

        @Deprecated("Deprecated in Java", ReplaceWith(
            "return super.shouldOverrideUrlLoading(view, request)",
            "androidx.webkit.WebViewClientCompat"
        ))
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return overrideUrlLoading(url)
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            webViewProgressIndicator.showView()
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            webViewProgressIndicator.hideView()
        }

        @RequiresApi(23)
        @UiThread
        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR)) return
            if (request.isForMainFrame) {
                onReceivedError()
            }
        }

        @Deprecated("Deprecated in Java", ReplaceWith(
            "super.onReceivedError(view, request, error)",
            "androidx.webkit.WebViewClientCompat"
        ))
        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            onReceivedError()
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.proceed()
        }

        @UiThread
        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_HTTP_ERROR)) {
                super.onReceivedHttpError(view, request, errorResponse)
                Log.e(TAG, "WebView ReceivedHttpError")
                Log.e(TAG, "${request.url}")
            }
        }
    }

    inner class CustomWebChromeClient : WebChromeClient() {

        private var hasShownCustomView: Boolean = false
        private var customView: View? = null
        private var defaultOrientation = 0
        private var defaultSystemUiVisibility = 0
        private var customViewCallback: CustomViewCallback? = null
        private var uploadCallback: ValueCallback<Array<Uri>>? = null

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            val result: WebView.HitTestResult? = view?.hitTestResult
            val data = result?.extra
            if (!isDialog) {
                val intent = Intent(context, BrowserActivity::class.java).apply {
                    putExtra("WEB_URL", data)
                }
                startActivity(intent)
            }
            return true
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            hasShownCustomView = true
            if (customView != null) {
                onHideCustomView()
                return
            }
            view?.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
            customView = view
            defaultSystemUiVisibility = getDefaultSystemUiVisibility()
            defaultOrientation = requestedOrientation
            customViewCallback = callback
            (window.decorView as FrameLayout).addView(customView, FrameLayout.LayoutParams(-1, -1))
            hideSystemUi()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                customView?.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets? ->
                    val suppliedInsets = v.onApplyWindowInsets(insets)
                    if (suppliedInsets.isVisible(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())) {
                        updateControls(0)
                    } else {
                        updateControls(getNavigationBarHeight())
                    }
                    suppliedInsets
                }
            } else {

                //Deprecated in Api level 30
                customView?.setOnSystemUiVisibilityChangeListener { visibility: Int ->
                    if (visibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION == 0) {
                        updateControls(getNavigationBarHeight())
                    } else {
                        updateControls(0)
                    }
                }

            }
        }

        override fun onHideCustomView() {
            hasShownCustomView = false
            (window.decorView as FrameLayout).removeView(customView)
            showSystemUi(defaultSystemUiVisibility)
            requestedOrientation = defaultOrientation
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        }

        @Nullable
        override fun getDefaultVideoPoster(): Bitmap? {
            return if (super.getDefaultVideoPoster() == null) {
                BitmapFactory.decodeResource(resources, R.drawable.img_poster_horizontal)
            } else super.getDefaultVideoPoster()
        }

        override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
            val allowMultiple: Boolean = fileChooserParams!!.mode ==FileChooserParams.MODE_OPEN_MULTIPLE
            if (uploadCallback != null) {
                uploadCallback!!.onReceiveValue(null)
                uploadCallback = null
            }
            uploadCallback = filePathCallback
            val chooserIntent: Intent = fileChooserParams.createIntent()
            chooserIntent.addCategory(Intent.CATEGORY_OPENABLE)
            if (allowMultiple) chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            try {
                activityResultLauncher.launch(chooserIntent)
            } catch (e: Exception) {
                Toast.makeText(context, getString(R.string.error_activity_not_found), Toast.LENGTH_LONG).show()
            }
            return true
        }

        // User defined functions

        private fun getDefaultSystemUiVisibility(): Int {
            val defaultSystemUiVisibility: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val insetsController = window.insetsController
                insetsController!!.systemBarsBehavior
            } else {

                // Deprecated in Api level 30
                window.decorView.systemUiVisibility

            }
            return defaultSystemUiVisibility
        }

        private fun hideSystemUi() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                controlWindowInsets(true)
            } else {

                // Deprecated in Api level 30
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN)

            }
        }

        private fun showSystemUi(defaultSystemUiVisibility: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                controlWindowInsets(false)
            } else {

                //Deprecated in Api level 30
                window.decorView.systemUiVisibility = defaultSystemUiVisibility

            }
        }

        @RequiresApi(30)
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

        private fun updateControls(bottomMargin: Int) {
            val params = customView!!.layoutParams as FrameLayout.LayoutParams
            params.bottomMargin = bottomMargin
            customView!!.layoutParams = params
        }

        private fun getNavigationBarHeight(): Int {
            val resources: Resources = resources
            val resourceId: Int =
                resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else 0
        }

        private val activityResultLauncher = registerForActivityResult(StartActivityForResult()) { result: ActivityResult ->
            when(result.resultCode) {
                RESULT_OK -> {
                    val intent = result.data
                    if (uploadCallback == null) return@registerForActivityResult
                    uploadCallback!!.onReceiveValue(FileChooserParams.parseResult(result.resultCode, intent))
                }
                else -> {
                    uploadCallback!!.onReceiveValue(null)
                }
            }
            uploadCallback = null
        }
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun openCustomTabs(url: String) {
        val colorInt: Int = ContextCompat.getColor(context, R.color.primary)
        val defaultColors: CustomTabColorSchemeParams = CustomTabColorSchemeParams.Builder()
            .setToolbarColor(colorInt)
            .build()
        val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
        builder.setDefaultColorSchemeParams(defaultColors)
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }

    private fun overrideUrlLoading(url: String?) : Boolean {
        if (URLUtil.isNetworkUrl(url)) {
            return false
        } else {
            when {
                url?.startsWith(CHROME_SCHEME) == true -> {
                    val replacedURL = URLDecoder.decode(url.substring(CHROME_SCHEME.length), "UTF-8")
                    openCustomTabs(replacedURL)
                }
                url?.startsWith(VIDEO_SCHEME) == true -> {
                    val replacedURL = URLDecoder.decode(url.substring(VIDEO_SCHEME.length), "UTF-8")
                    if (replacedURL.startsWith("https://youtu.be/")) {
                        val intent = Intent(context, YouTubePlayerActivity::class.java).apply {
                            putExtra("VIDEO_URL", replacedURL)
                        }
                        startActivity(intent)
                    } else {
                        playerUtil.loadPlayer(context, Uri.parse(replacedURL), true)
                    }
                }
                url?.startsWith(INTENT_SCHEME) == true -> {
                    try {
                        val handlerIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (handlerIntent != null) {
                            val packageManager = context.packageManager
                            val info = packageManager.resolveActivity(handlerIntent, PackageManager.MATCH_DEFAULT_ONLY)
                            if (info != null) {
                                context.startActivity(handlerIntent)
                            } else {
                                val marketIntent = Intent(Intent.ACTION_VIEW)
                                marketIntent.data = Uri.parse("market://details?id=" + handlerIntent.getPackage())
                                try {
                                    startActivity(marketIntent)
                                } catch (notFoundException: ActivityNotFoundException) {
                                    Toast.makeText(context, "Failed to load URL", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } catch (uriSyntaxException: URISyntaxException) {
                        Toast.makeText(context, "Failed to load URL", Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    try {
                        val unknownURLIntent = Intent(Intent.ACTION_VIEW)
                        unknownURLIntent.data = Uri.parse(url)
                        startActivity(unknownURLIntent)
                    } catch (e: Exception) {
                        if (url?.startsWith(TORRENT_SCHEME) == true) {
                            Toast.makeText(context, "Download a torrent client and Try again!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Unsupported URL", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        return true
    }

    private fun onReceivedError() {
        webView.evaluateJavascript("javascript:document.open();document.write('');document.close();", null)
        webView.hideView()
        webViewErrorOccurred = true
        errorTextView.text = resources.getString(R.string.error_failed_to_load_content)
        errorView.showView()
    }

    private fun fetchJavaScript(url: String?) {
        // JavaScript code to fetch() content from the same origin
        val jsCode = "fetch('$url')" +
                ".then(resp => resp.json())" +
                ".then(data => console.log(data));"
        webView.evaluateJavascript(jsCode, null)
    }
}