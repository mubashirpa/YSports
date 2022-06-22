package ysports.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
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
import android.location.LocationManager
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.webkit.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import ysports.app.databinding.ActivityWebBinding
import ysports.app.player.PlayerUtil
import ysports.app.util.AdBlocker
import ysports.app.util.AppUtil
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
    private var LOCATION_PERMISSION_REQUEST_CODE = 101
    private var CAMERA_PERMISSION_REQUEST_CODE = 102
    private var MIC_PERMISSION_REQUEST_CODE = 103
    private var permissionRequest: PermissionRequest? = null
    private var geolocationCallback: GeolocationPermissions.Callback? = null
    private var geolocationOrigin: String? = null

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
        WEB_URL = intent.getStringExtra("WEB_URL") ?: resources.getString(R.string.url_404_error)

        webView.setBackgroundColor(Color.TRANSPARENT)
        AdBlocker.init(context)

        webView.setOnLongClickListener {
            true
        }

        retryButton.setOnClickListener {
            onReload()
        }

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

        webView.setDownloadListener { url, _, _, _, _ ->
            val downloadIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            try {
                startActivity(downloadIntent)
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
            }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Camera permission denied")
                onPermissionRequestConfirmation(false, arrayOf(""))
            } else {
                onPermissionRequestConfirmation(true, arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
            }
        }
        if (requestCode == MIC_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Microphone permission denied")
                onPermissionRequestConfirmation(false, arrayOf(""))
            } else {
                onPermissionRequestConfirmation(true, arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            }
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Location permission denied")
                onGeolocationPermissionConfirmation(geolocationOrigin, allowed = false, retain = false)
            } else {
                fetchLocation()
            }
        }
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
            errorView.hideView()
            webViewProgressIndicator.showView()
            webView.showView()
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
                Log.e(TAG, "WebView ReceivedHttpError: ${request.url}")
            }
        }
    }

    inner class CustomWebChromeClient : WebChromeClient() {

        private var hasShownCustomView: Boolean = false
        private var customView: View? = null
        private var defaultOrientation = 0
        private var defaultSystemUiVisibility = 0
        private var customViewCallback: CustomViewCallback? = null
        private var pathCallback: ValueCallback<Array<Uri>>? = null

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

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            MaterialAlertDialogBuilder(context)
                .setMessage(message)
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    result?.confirm()
                }
                .setOnCancelListener {
                    result?.cancel()
                }
                .show()
            return true
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            MaterialAlertDialogBuilder(context)
                .setMessage(message)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                    result?.cancel()
                }
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    result?.confirm()
                }
                .setOnCancelListener {
                    result?.cancel()
                }
                .show()
            return true
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
            val inputLayout: View = layoutInflater.inflate(R.layout.view_input_view_dialog, null)
            val textInputEditText: TextInputEditText = inputLayout.findViewById(R.id.input_text)
            textInputEditText.maxLines = 1
            textInputEditText.setText(defaultValue)
            MaterialAlertDialogBuilder(context)
                .setMessage(message)
                .setView(inputLayout)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                    result?.cancel()
                }
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    if (textInputEditText.text.isNullOrEmpty()) {
                        result?.confirm()
                    } else {
                        result?.confirm(textInputEditText.text.toString())
                    }
                }
                .setOnCancelListener {
                    result?.cancel()
                }
                .show()
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest?) {
            permissionRequest = null
            if (request != null) {
                permissionRequest = request
                val requestedResources = request.resources
                for (reqResources in requestedResources) {
                    Log.d(TAG, "Permission request: $reqResources")
                    when (reqResources) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            requestWebViewPermission(
                                Manifest.permission.CAMERA,
                                CAMERA_PERMISSION_REQUEST_CODE,
                                "YSports need camera permission to access camera",
                                arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                            )
                        }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            requestWebViewPermission(
                                Manifest.permission.RECORD_AUDIO,
                                MIC_PERMISSION_REQUEST_CODE,
                                "YSports need microphone permission to access mic",
                                arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                            )
                        }
                        else -> {
                            onPermissionRequestConfirmation(false, arrayOf(""))
                        }
                    }
                }
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            Log.d(TAG, "Permission request cancelled")
            permissionRequest = null
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
            geolocationCallback = null
            geolocationOrigin = null
            geolocationCallback = callback
            geolocationOrigin = origin
            requestLocationPermission()
        }

        override fun onGeolocationPermissionsHidePrompt() {
            Log.d(TAG, "Geolocation permission prompt hide")
            geolocationCallback = null
            geolocationOrigin = null
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
            if (fileChooserParams != null) {
                val allowMultiple: Boolean = fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                if (pathCallback != null) {
                    pathCallback!!.onReceiveValue(null)
                    pathCallback = null
                }
                pathCallback = filePathCallback
                val chooserIntent: Intent = fileChooserParams.createIntent().apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    if (allowMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                activityResultLauncher.launch(chooserIntent)
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

        private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (pathCallback != null) {
                var results: Array<Uri>? = null
                if (result.resultCode == Activity.RESULT_OK) {
                    val dataString = result.data?.dataString
                    if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
                pathCallback!!.onReceiveValue(results)
                pathCallback = null
            }
        }
    }

    private fun overrideUrlLoading(url: String?) : Boolean {
        if (URLUtil.isNetworkUrl(url)) {
            return false
        } else {
            when {
                url?.startsWith(CHROME_SCHEME) == true -> {
                    val replacedURL = URLDecoder.decode(url.substring(CHROME_SCHEME.length), "UTF-8")
                    AppUtil(context).openCustomTabs(replacedURL)
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
        errorTextView.text = resources.getString(R.string.error_failed_to_load_content)
        errorView.showView()
    }

    private fun onReload() {
        errorView.hideView()
        webViewProgressIndicator.showView()
        handler = Handler(Looper.getMainLooper())
        handler!!.postDelayed({
            if (!isDestroyed) {
                webView.showView()
                webView.reload()
            }
        }, 300)
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun isPermissionGranted(permission: String) : Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWebViewPermission(permission: String, requestCode: Int, message: String, requestResources: Array<String>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                isPermissionGranted(permission) -> {
                    Log.d(TAG, "Permission already granted")
                    onPermissionRequestConfirmation(true, requestResources)
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Allow permission?")
                        .setMessage(message)
                        .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                            onPermissionRequestConfirmation(false, arrayOf(""))
                        }
                        .setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                            requestPermissions(arrayOf(permission), requestCode)
                        }
                        .setCancelable(false)
                        .show()
                }
                else -> {
                    requestPermissions(arrayOf(permission), requestCode)
                }
            }
        } else {
            if (isPermissionGranted(permission)) {
                onPermissionRequestConfirmation(true, requestResources)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        }
    }

    private fun onPermissionRequestConfirmation(allowed: Boolean, resources: Array<String>) {
        if (permissionRequest != null) {
            if (allowed) {
                permissionRequest!!.grant(resources)
                Log.d(TAG, "Permission granted")
            } else {
                permissionRequest!!.deny()
                Log.e(TAG, "Permission request denied")
            }
            permissionRequest = null
        }
    }

    private fun isLocationServiceEnabled() : Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    Log.d(TAG, "Permission already granted (Fine)")
                    fetchLocation()
                }
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                    Log.d(TAG, "Permission already granted (Coarse")
                    fetchLocation()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Allow permission?")
                        .setMessage("YSports need location permission to access location")
                        .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                            onGeolocationPermissionConfirmation(geolocationOrigin, allowed = false, retain = false)
                        }
                        .setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                            requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE)
                        }
                        .setCancelable(false)
                        .show()
                }
                else -> {
                    requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE)
                }
            }
        } else {
            if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
                || isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                fetchLocation()
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun onGeolocationPermissionConfirmation(origin: String?, allowed: Boolean, retain: Boolean) {
        if (geolocationCallback != null) {
            geolocationCallback?.invoke(origin, allowed, retain)
            geolocationCallback = null
            geolocationOrigin = null
        }
    }

    private fun fetchLocation() {
        if (!isLocationServiceEnabled()) {
            onGeolocationPermissionConfirmation(geolocationOrigin, allowed = false, retain = false)
            Toast.makeText(context, "Please enable location services in settings", Toast.LENGTH_LONG).show()
            val locationIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            try {
                startActivity(locationIntent)
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
            }
        } else {
            onGeolocationPermissionConfirmation(geolocationOrigin, allowed = true, retain = false)
        }
    }
}