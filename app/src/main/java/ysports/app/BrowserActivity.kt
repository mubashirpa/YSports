package ysports.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.webkit.*
import androidx.webkit.WebSettingsCompat.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import ysports.app.databinding.ActivityBrowserBinding
import ysports.app.player.PlayerUtil
import ysports.app.util.AdBlocker
import ysports.app.util.AppUtil
import ysports.app.util.YouTubePlay
import java.net.URISyntaxException

@Suppress("PrivatePropertyName")
class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private lateinit var context: Context
    private lateinit var webView: WebView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var toolbar: MaterialToolbar
    private lateinit var WEB_URL: String
    private var handler: Handler? = null
    private val TAG = "BrowserActivity"
    private val INTENT_SCHEME = "intent:"
    private val TORRENT_SCHEME = "magnet:"
    private var errorDescription: String = "Unknown"
    private var errorCode: Int = 0
    private var urlHost = "YSports"
    private var LOCATION_PERMISSION_REQUEST_CODE = 101
    private var CAMERA_PERMISSION_REQUEST_CODE = 102
    private var MIC_PERMISSION_REQUEST_CODE = 103
    private var permissionRequest: PermissionRequest? = null
    private var geolocationCallback: GeolocationPermissions.Callback? = null
    private var geolocationOrigin: String? = null
    private var enableSafeBrowsing = false
    private var enableAdBlocker = true
    private var enablePopupBlocker = true
    private var allowCookie = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        toolbar = binding.materialToolbar
        webView = binding.webView
        progressBar = binding.progressBar
        WEB_URL = intent.getStringExtra("WEB_URL") ?: "https://ysports.app/assets/web/error_404/index.html"

        AdBlocker.init(context)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.share -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, webView.url ?: WEB_URL)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Link"))
                    true
                }
                R.id.refresh -> {
                    binding.errorLayout.hideView()
                    webView.showView()
                    webView.reload()
                    true
                }
                R.id.copy -> {
                    (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("clipboard", webView.url ?: WEB_URL))
                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.open -> {
                    AppUtil(context).openCustomTabs(webView.url ?: WEB_URL)
                    true
                }
                else -> false
            }
        }

        binding.reloadButton.setOnClickListener {
            onReload()
        }

        binding.detailsButton.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(webView.url)
                .setMessage("Error: $errorDescription\n\nError code:$errorCode")
                .setPositiveButton(resources.getString(R.string.cancel)) { _, _ -> }
                .show()
        }

        webView.setOnLongClickListener {
            val hitTestResult = webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                val items = arrayOf("Open in Browser", "Copy URL", "Share link")
                MaterialAlertDialogBuilder(context)
                    .setTitle(hitTestResult.extra)
                    .setItems(items) { _, position ->
                        when (position) {
                            0 -> AppUtil(context).openCustomTabs(hitTestResult.extra.toString())
                            1 -> {
                                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("clipboard", hitTestResult.extra))
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            2 -> {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, hitTestResult.extra)
                                }
                                startActivity(Intent.createChooser(shareIntent, "Share Link"))
                            }
                        }
                    }
                    .show()
            }
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE || hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                val items = arrayOf("Open in Browser", "Preview image", "Download image", "Copy URL", "Share link")
                MaterialAlertDialogBuilder(context)
                    .setTitle(hitTestResult.extra)
                    .setItems(items) { _, position ->
                        when (position) {
                            0 -> AppUtil(context).openCustomTabs(hitTestResult.extra.toString())
                            1 -> {
                                val intent = Intent(context, BrowserActivity::class.java).apply {
                                    putExtra("WEB_URL", hitTestResult.extra)
                                }
                                startActivity(intent)
                            }
                            2 -> {
                                // TODO("Download image")
                            }
                            3 -> {
                                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("clipboard", hitTestResult.extra))
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            4 -> {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, hitTestResult.extra)
                                }
                                startActivity(Intent.createChooser(shareIntent, "Share Link"))
                            }
                        }
                    }
                    .show()
            }
            false
        }

        // WebView Version API
        val webViewPackageInfo = WebViewCompat.getCurrentWebViewPackage(context)
        Log.d(TAG, "WebView version: ${webViewPackageInfo?.versionName}")

        // Initialize safe browsing
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            if (enableSafeBrowsing) {
                WebViewCompat.startSafeBrowsing(this) { success ->
                    if (success) {
                        Log.e(TAG, "Initialized Safe Browsing")
                    } else {
                        Log.e(TAG, "Unable to initialize Safe Browsing!")
                    }
                }
            }
        }

        // WebViewAssetLoader
        // Default URL: https://appassets.androidplatform.net/assets/index.html
        // Custom URL: https://ysports.app/assets/index.html
        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("ysports.app")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
            .build()

        // Supporting Dark Theme for WebView
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    setForceDark(webView.settings, FORCE_DARK_ON)
                }
                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    setForceDark(webView.settings, FORCE_DARK_OFF)
                }
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            setForceDarkStrategy(webView.settings, DARK_STRATEGY_WEB_THEME_DARKENING_ONLY)
        }

        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews for reference
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            useWideViewPort = true
            userAgentString = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/101.0.4951.61 Mobile Safari/537.36"
        }
        webView.isSoundEffectsEnabled = true
        webView.isLongClickable = true
        webView.requestFocusFromTouch()

        // Renderer Importance API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, true)
        }
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.webViewClient = CustomWebViewClient(assetLoader)
        webView.webChromeClient = CustomWebChromeClient()
        webView.loadUrl(WEB_URL)

        webView.setDownloadListener { url, _, _, _, _ ->
            val downloadIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            startActivity(downloadIntent)
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
        if (allowCookie) {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }
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
    }

    @Suppress("unused")
    inner class WebAppInterface {

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
                PlayerUtil().loadPlayer(context, Uri.parse(url), true)
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

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        override fun onSafeBrowsingHit(view: WebView, request: WebResourceRequest, threatType: Int, callback: SafeBrowsingResponseCompat) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY)) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("Unsafe")
                    .setMessage("Unsafe web page detected. Do you want to block it?")
                    .setNegativeButton(resources.getString(R.string.continue_page)) { _, _ ->
                        callback.backToSafety(false)
                    }
                    .setPositiveButton(resources.getString(R.string.block)) { _, _ ->
                        callback.backToSafety(true)
                        Toast.makeText(view.context, "Unsafe web page blocked.", Toast.LENGTH_LONG).show()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        // Termination Handling API
        @RequiresApi(26)
        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            if (detail != null && !detail.didCrash()) {
                // Renderer was killed because the system ran out of memory.
                // The app can recover gracefully by creating a new WebView instance in the foreground.
                Log.e(TAG, "System killed the WebView rendering process to reclaim memory. Recreating...")

                webView.also { web ->
                    web.destroy()
                }
                return true // The app continues executing.
            }

            // Renderer crashed because of an internal error, such as a memory access violation.
            Log.e(TAG, "The WebView rendering process crashed!")
            return false
        }

        // API >= 21
        @Nullable
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            if (!enableAdBlocker) return assetLoader.shouldInterceptRequest(request.url)
            val loadedUrls: MutableMap<String, Boolean> = HashMap()
            val resourceUrl = request.url.toString()
            val ad: Boolean
            if (!loadedUrls.containsKey(resourceUrl)) {
                ad = AdBlocker.isAd(resourceUrl)
                loadedUrls[resourceUrl] = ad
            } else {
                ad = loadedUrls[resourceUrl]!!
            }
            return if (ad) AdBlocker.createEmptyResource() else return assetLoader.shouldInterceptRequest(request.url)
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
            binding.errorLayout.hideView()
            progressBar.showView()
            webView.showView()
            errorDescription = "Unknown"
            errorCode = 0
            urlHost = Uri.parse(url).host.toString()
            toolbar.subtitle = urlHost
        }

        @RequiresApi(23)
        @UiThread
        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR)) return
            if (request.isForMainFrame) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION)) errorDescription = error.description.toString()
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE)) errorCode = error.errorCode
                onReceivedError()
            }
        }

        @Deprecated("Deprecated in Java", ReplaceWith(
            "super.onReceivedError(view, request, error)",
            "androidx.webkit.WebViewClientCompat"
        ))
        override fun onReceivedError(view: WebView?, code: Int, description: String?, failingUrl: String?) {
            if (description != null) errorDescription = description
            errorCode = code
            onReceivedError()
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            var errorMessage = "Certificate error."
            when (error?.primaryError) {
                SslError.SSL_UNTRUSTED -> errorMessage = "The certificate authority is not trusted."
                SslError.SSL_EXPIRED -> errorMessage = "The certificate has been expired."
                SslError.SSL_IDMISMATCH -> errorMessage = "The certificate hostname mismatch."
                SslError.SSL_NOTYETVALID -> errorMessage = "The certificate is not yet valid."
                SslError.SSL_DATE_INVALID -> errorMessage = "The certificate date is invalid"
            }
            MaterialAlertDialogBuilder(context)
                .setTitle("SSL Certificate Error")
                .setMessage(errorMessage)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                    handler?.cancel()
                }
                .setPositiveButton(resources.getString(R.string.proceed)) { _, _ ->
                    handler?.proceed()
                }
                .setCancelable(false)
                .show()
        }

        @UiThread
        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_HTTP_ERROR)) {
                super.onReceivedHttpError(view, request, errorResponse)
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

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            progressBar.progress = newProgress
            if (newProgress == 100) progressBar.hideView()
        }

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            if (!isUserGesture && enablePopupBlocker) {
                Snackbar.make(binding.contextView, "Pop-up blocked", Snackbar.LENGTH_LONG).show()
                return false
            }
            val result: WebView.HitTestResult? = view?.hitTestResult
            val url = result?.extra
            if (!isDialog) {
                val intent = Intent(context, BrowserActivity::class.java).apply {
                    putExtra("WEB_URL", url)
                }
                startActivity(intent)
            }
            return true
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            if (!title.isNullOrEmpty()) toolbar.title = title
        }

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            val host = Uri.parse(webView.url).host
            MaterialAlertDialogBuilder(context)
                .setTitle("$host says")
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
            val host = Uri.parse(webView.url).host
            MaterialAlertDialogBuilder(context)
                .setTitle("$host says")
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

        @SuppressLint("InflateParams")
        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
            val host = Uri.parse(webView.url).host
            val inputLayout: View = layoutInflater.inflate(R.layout.view_input_view_dialog, null)
            val textInputEditText: TextInputEditText = inputLayout.findViewById(R.id.input_text)
            textInputEditText.maxLines = 1
            textInputEditText.setText(defaultValue)
            MaterialAlertDialogBuilder(context)
                .setTitle("$host says")
                .setMessage(message)
                .setView(inputLayout)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                    result?.cancel()
                }
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    if (textInputEditText.text.isNullOrEmpty()) {
                        result?.confirm()
                    } else {
                        result?.confirm(defaultValue)
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
            permissionRequest = request
            if (request != null) {
                val requestedResources = request.resources
                for (reqResources in requestedResources) {
                    Log.d(TAG, "Permission request: $reqResources")
                    when (reqResources) {
                        PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                            MaterialAlertDialogBuilder(context)
                                .setMessage("$urlHost wants to use your camera")
                                .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                                    onPermissionRequestConfirmation(false, arrayOf(""))
                                }
                                .setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                                    if (isPermissionGranted(Manifest.permission.CAMERA)) {
                                        onPermissionRequestConfirmation(true, arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                                    } else {
                                        requestWebViewPermission(
                                            Manifest.permission.CAMERA,
                                            CAMERA_PERMISSION_REQUEST_CODE,
                                            "YSports need camera permission for this site"
                                        )
                                    }
                                }
                                .setOnCancelListener {
                                    onPermissionRequestConfirmation(false, arrayOf(""))
                                }
                                .show()
                        }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            MaterialAlertDialogBuilder(context)
                                .setMessage("$urlHost wants to use your microphone")
                                .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                                    onPermissionRequestConfirmation(false, arrayOf(""))
                                }
                                .setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                                    if (isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
                                        onPermissionRequestConfirmation(true, arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                                    } else {
                                        requestWebViewPermission(
                                            Manifest.permission.RECORD_AUDIO,
                                            MIC_PERMISSION_REQUEST_CODE,
                                            "YSports need microphone permission for this site"
                                        )
                                    }
                                }
                                .setOnCancelListener {
                                    onPermissionRequestConfirmation(false, arrayOf(""))
                                }
                                .show()
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
            MaterialAlertDialogBuilder(context)
                .setMessage("$urlHost wants to use your devices location")
                .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                    onGeolocationPermissionConfirmation(geolocationOrigin, allowed = false, retain = false)
                }
                .setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                    if (isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) || isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
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
                    } else {
                        requestLocationPermission()
                    }
                }
                .setOnCancelListener {
                    onGeolocationPermissionConfirmation(geolocationOrigin, allowed = false, retain = false)
                }
                .show()
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
        if (url.isNullOrEmpty()) return false
        if (URLUtil.isNetworkUrl(url)) {
            return false
        } else {
            when {
                url.startsWith(INTENT_SCHEME) -> {
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
                        if (url.startsWith(TORRENT_SCHEME)) {
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
        binding.errorLayout.showView()
    }

    private fun onReload() {
        binding.errorLayout.hideView()
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

    private fun fetchJavaScript(url: String?) {
        // JavaScript code to fetch() content from the same origin
        val jsCode = "fetch('$url')" +
                ".then(resp => resp.json())" +
                ".then(data => console.log(data));"
        webView.evaluateJavascript(jsCode, null)
    }

    private fun isPermissionGranted(permission: String) : Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWebViewPermission(permission: String, requestCode: Int, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                isPermissionGranted(permission) -> {
                    Log.d(TAG, "Permission already granted")
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
            if (!isPermissionGranted(permission)) {
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
                }
                isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                    Log.d(TAG, "Permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Allow permission?")
                        .setMessage("YSports need location permission for this site")
                        .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
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
            if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)
                || !isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)) {
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
}