package ysports.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.net.http.SslError
import android.os.*
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Log
import android.view.View
import android.webkit.*
import android.webkit.WebView.RENDERER_PRIORITY_BOUND
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.webkit.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import ysports.app.databinding.ActivityBrowserBinding
import ysports.app.player.PlayerUtil
import ysports.app.ui.bottomsheet.menu.BottomSheetMenu
import ysports.app.util.AppUtil
import ysports.app.util.NetworkUtil
import ysports.app.webview.AdBlocker
import ysports.app.webview.WebAppInterface
import java.io.File
import java.io.FileOutputStream
import java.net.URISyntaxException
import java.util.*
import kotlin.math.abs


@Suppress("PrivatePropertyName")
class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private lateinit var context: Context
    private lateinit var webView: WebView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var toolbar: MaterialToolbar
    private lateinit var WEB_URL: String
    private val TAG = "LogBrowserActivity"
    private val INTENT_SCHEME = "intent:"
    private val TORRENT_SCHEME = "magnet:"
    private val BLOB_SCHEME = "blob:"
    private val LOCATION_PERMISSION_REQUEST_CODE = 101
    private val CAMERA_PERMISSION_REQUEST_CODE = 102
    private val MIC_PERMISSION_REQUEST_CODE = 103
    private var errorDescription: String = "Unknown"
    private var errorCode: Int = 0
    private var urlHost = "YSports"
    private var handler: Handler? = null
    private var permissionRequest: PermissionRequest? = null
    private var geolocationCallback: GeolocationPermissions.Callback? = null
    private var geolocationOrigin: String? = null
    private var enableSafeBrowsing = false
    private var enableAdBlocker = true
    private var enablePopupBlocker = true
    private var allowCookie = true
    private lateinit var downloadManager: DownloadManager
    private var downloadReference: Long? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        toolbar = binding.materialToolbar
        webView = binding.webView
        progressBar = binding.progressBar
        WEB_URL = intent.getStringExtra("WEB_URL") ?: resources.getString(R.string.url_404_error)
        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        urlHost = Uri.parse(WEB_URL).host.toString()
        toolbar.subtitle = if (urlHost == "null") WEB_URL else urlHost
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)

        registerReceiver(downloadReceiver, filter)
        AdBlocker.init(context)
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.share -> {
                    shareText(getString(R.string.share_link), webView.title, webView.url ?: WEB_URL)
                    true
                }
                R.id.refresh -> {
                    binding.errorLayout.hideView()
                    webView.showView()
                    webView.reload()
                    true
                }
                R.id.copy -> {
                    (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                        ClipData.newPlainText("clipboard", webView.url ?: WEB_URL)
                    )
                    Toast.makeText(
                        context, getString(R.string.link_copied_to_clipboard), Toast.LENGTH_SHORT
                    ).show()
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
            MaterialAlertDialogBuilder(context).setTitle(webView.url)
                .setMessage("Error: $errorDescription\n\nError code:$errorCode")
                .setPositiveButton(resources.getString(R.string.cancel)) { _, _ -> }.show()
        }

        webView.setOnLongClickListener {
            val hitTestResult = webView.hitTestResult
            val srcExtra = hitTestResult.extra
            val message = Handler(Looper.getMainLooper()).obtainMessage()
            webView.requestFocusNodeHref(message)
            val title = message.data.getString("title")?.trim()
            val url = message.data.getString("url")
            val menuHeader: View = layoutInflater.inflate(R.layout.header_webview_menu, null)
            val menuIcon: ImageView = menuHeader.findViewById(R.id.icon)
            val menuTitle: TextView = menuHeader.findViewById(R.id.title)
            val menuSubtitle: TextView = menuHeader.findViewById(R.id.subtitle)

            if (hitTestResult.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                val items = resources.getStringArray(R.array.web_src_anchor_type_items)
                menuIcon.setImageBitmap(webView.favicon)
                if (!title.isNullOrEmpty()) {
                    menuTitle.text = title
                    menuTitle.visibility = View.VISIBLE
                }
                menuSubtitle.text = srcExtra
                val bottomSheetMenu = showLongClickMenu(items, menuHeader)
                bottomSheetMenu.setOnItemClickListener { _, _, position, _ ->
                    when (position) {
                        1 -> if (srcExtra != null) AppUtil(context).openCustomTabs(srcExtra)
                        2 -> {
                            if (srcExtra != null) {
                                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                    ClipData.newPlainText("clipboard", srcExtra)
                                )
                                Toast.makeText(
                                    context,
                                    getString(R.string.link_copied_to_clipboard),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        3 -> {
                            if (title != null) {
                                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                    ClipData.newPlainText("clipboard", title)
                                )
                                Toast.makeText(
                                    context,
                                    getString(R.string.text_copied_to_clipboard),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        4 -> if (srcExtra != null) shareText(
                            getString(R.string.share_via), title, srcExtra
                        )
                    }
                    bottomSheetMenu.dismiss()
                }
                bottomSheetMenu.show(supportFragmentManager, BottomSheetMenu.TAG)
            }
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE) {
                val items = resources.getStringArray(R.array.web_image_anchor_type_items)
                Glide.with(context).load(srcExtra).into(menuIcon)
                if (!title.isNullOrEmpty()) {
                    menuTitle.text = title
                    menuTitle.visibility = View.VISIBLE
                }
                menuSubtitle.text = srcExtra
                val bottomSheetMenu = showLongClickMenu(items, menuHeader)
                bottomSheetMenu.setOnItemClickListener { _, _, position, _ ->
                    when (position) {
                        1 -> if (srcExtra != null) AppUtil(context).openCustomTabs(srcExtra)
                        2 -> {
                            val intent = Intent(context, BrowserActivity::class.java).apply {
                                putExtra("WEB_URL", srcExtra)
                            }
                            startActivity(intent)
                        }
                        3 -> if (srcExtra != null) downloadReference = startDownload(srcExtra)
                        4 -> if (srcExtra != null) {
                            shareImageFromUrl(srcExtra)
                        }
                    }
                    bottomSheetMenu.dismiss()
                }
                bottomSheetMenu.show(supportFragmentManager, BottomSheetMenu.TAG)
            }
            if (hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                val items = resources.getStringArray(R.array.web_src_image_anchor_type_items)
                Glide.with(context).load(srcExtra).into(menuIcon)
                if (!title.isNullOrEmpty()) {
                    menuTitle.text = title
                    menuTitle.visibility = View.VISIBLE
                }
                menuSubtitle.text = url
                val bottomSheetMenu = showLongClickMenu(items, menuHeader)
                bottomSheetMenu.setOnItemClickListener { _, _, position, _ ->
                    when (position) {
                        1 -> if (url != null) AppUtil(context).openCustomTabs(url)
                        2 -> {
                            if (url != null) {
                                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                                    ClipData.newPlainText("clipboard", url)
                                )
                                Toast.makeText(
                                    context,
                                    getString(R.string.link_copied_to_clipboard),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        3 -> if (url != null) shareText(getString(R.string.share_via), null, url)

                        4 -> {
                            val intent = Intent(context, BrowserActivity::class.java).apply {
                                putExtra("WEB_URL", srcExtra)
                            }
                            startActivity(intent)
                        }
                        5 -> if (srcExtra != null) downloadReference = startDownload(srcExtra)
                        6 -> if (srcExtra != null) {
                            shareImageFromUrl(srcExtra)
                        }
                    }
                    bottomSheetMenu.dismiss()
                }
                bottomSheetMenu.show(supportFragmentManager, BottomSheetMenu.TAG)
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
                        Log.i(TAG, "Initialized Safe Browsing")
                    } else {
                        Log.e(TAG, "Unable to initialize Safe Browsing!")
                    }
                }
            }
        }

        // WebViewAssetLoader
        // Default URL: https://appassets.androidplatform.net/assets/index.html
        // Custom URL: https://ysports.app/assets/index.html
        val assetLoader = WebViewAssetLoader.Builder().setDomain("ysports.app")
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this)).build()

        // Supporting Dark Theme for WebView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) WebSettingsCompat.setAlgorithmicDarkeningAllowed(
                webView.settings,
                false
            )
        } else {
            // Deprecated in Api level 33
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) WebSettingsCompat.setForceDark(
                        webView.settings, WebSettingsCompat.FORCE_DARK_ON
                    )
                }
                Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_UNDEFINED -> {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) WebSettingsCompat.setForceDark(
                        webView.settings, WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) WebSettingsCompat.setForceDarkStrategy(
                webView.settings, WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY
            )
        }

        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews for reference
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(false)
        }

        // WebView settings
        webView.settings.apply {
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
            offscreenPreRaster = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            useWideViewPort = true
            userAgentString = getString(R.string.web_user_agent, Build.VERSION.RELEASE, Build.MODEL)
        }
        webView.isSoundEffectsEnabled = true
        webView.isLongClickable = true
        webView.requestFocusFromTouch()

        // Renderer Importance API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, true)
        }
        webView.addJavascriptInterface(WebAppInterface(context, this), "Android")
        webView.webViewClient = CustomWebViewClient(assetLoader)
        webView.webChromeClient = CustomWebChromeClient()
        webView.loadUrl(WEB_URL)

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val downloadDialogBuilder = MaterialAlertDialogBuilder(context)
            val downloadLayout: View =
                layoutInflater.inflate(R.layout.view_browser_download_confirm, null)
            val textFileName: TextView = downloadLayout.findViewById(R.id.fileName)
            val textFileSize: TextView = downloadLayout.findViewById(R.id.fileSize)
            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
            val fileSize = Formatter.formatFileSize(context, contentLength)

            textFileName.text = fileName
            textFileSize.text = fileSize

            if (mimetype.contains("video")) {
                downloadDialogBuilder.setNeutralButton(resources.getString(R.string.play)) { _, _ ->
                    PlayerUtil().loadPlayer(context, Uri.parse(url), fileName, true)
                }
            }
            val wifiConnected = NetworkUtil().wifiConnected(context)
            if (!wifiConnected) downloadDialogBuilder.setMessage(getString(R.string.warning_cellular_data_usage))

            downloadDialogBuilder.setTitle(resources.getString(R.string.download))
                .setView(downloadLayout)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
                .setPositiveButton(resources.getString(R.string.download)) { _, _ ->
                    if (url.startsWith(BLOB_SCHEME)) {
                        convertBlobToUri(url, mimetype, fileName)
                    } else {
                        downloadReference = if (textFileName.text.isNullOrEmpty()) {
                            startDownload(url, userAgent, mimetype, fileName)
                        } else {
                            startDownload(url, userAgent, mimetype, textFileName.text.toString())
                        }
                    }
                }.setCancelable(false).show()
        }
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                webView.canGoBack() -> webView.goBack()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacksAndMessages(null)
        webView.destroy()
        try {
            unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            Log.e(TAG, e.message.toString(), e.cause)
        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Camera permission denied")
                onPermissionRequestConfirmation(false, arrayOf(""))
            } else {
                onPermissionRequestConfirmation(
                    true, arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                )
            }
        }
        if (requestCode == MIC_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Microphone permission denied")
                onPermissionRequestConfirmation(false, arrayOf(""))
            } else {
                onPermissionRequestConfirmation(
                    true, arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                )
            }
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Log.e(TAG, "Location permission denied")
                onGeolocationPermissionConfirmation(
                    geolocationOrigin, allowed = false, retain = false
                )
            } else {
                fetchLocation()
            }
        }
    }

    inner class CustomWebViewClient(private val assetLoader: WebViewAssetLoader) :
        WebViewClientCompat() {

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        override fun onSafeBrowsingHit(
            view: WebView,
            request: WebResourceRequest,
            threatType: Int,
            callback: SafeBrowsingResponseCompat
        ) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY)) {
                MaterialAlertDialogBuilder(context).setTitle(getString(R.string.warning_unsafe_page_title))
                    .setMessage(getString(R.string.warning_unsafe_page_message))
                    .setNegativeButton(resources.getString(R.string.continue_page)) { _, _ ->
                        callback.backToSafety(false)
                    }.setPositiveButton(resources.getString(R.string.block)) { _, _ ->
                        callback.backToSafety(true)
                        Toast.makeText(
                            view.context, getString(R.string.unsafe_page_blocked), Toast.LENGTH_LONG
                        ).show()
                    }.setCancelable(false).show()
            }
        }

        // Termination Handling API
        @RequiresApi(26)
        override fun onRenderProcessGone(
            view: WebView?, detail: RenderProcessGoneDetail?
        ): Boolean {
            if (detail != null && !detail.didCrash()) {
                // Renderer was killed because the system ran out of memory.
                // The app can recover gracefully by creating a new WebView instance in the foreground.
                Log.e(
                    TAG,
                    "System killed the WebView rendering process to reclaim memory. Recreating..."
                )

                webView.also { web ->
                    web.destroy()
                }
                return true // The app continues executing.
            }

            // Renderer crashed because of an internal error, such as a memory access violation.
            Log.e(TAG, "The WebView rendering process crashed!")
            return false
        }

        override fun shouldInterceptRequest(
            view: WebView, request: WebResourceRequest
        ): WebResourceResponse? {
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
            return if (ad) AdBlocker.createEmptyResource() else return assetLoader.shouldInterceptRequest(
                request.url
            )
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.SHOULD_OVERRIDE_WITH_REDIRECTS)) return false
            return overrideUrlLoading(request.url.toString())
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            binding.errorLayout.hideView()
            progressBar.showView()
            webView.showView()
            errorDescription = "Unknown"
            errorCode = 0
            urlHost = Uri.parse(url).host.toString()
            toolbar.subtitle = if (urlHost == "null") WEB_URL else urlHost
        }

        @UiThread
        override fun onReceivedError(
            view: WebView, request: WebResourceRequest, error: WebResourceErrorCompat
        ) {
            if (!WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_WEB_RESOURCE_ERROR)) return
            if (request.isForMainFrame) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION)) errorDescription =
                    error.description.toString()
                if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE)) errorCode =
                    error.errorCode
                onReceivedError()
            }
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(
            view: WebView?, handler: SslErrorHandler?, error: SslError?
        ) {
            val errorMessage = when (error?.primaryError) {
                SslError.SSL_UNTRUSTED -> getString(R.string.error_ssl_untrusted)
                SslError.SSL_EXPIRED -> getString(R.string.error_ssl_expired)
                SslError.SSL_IDMISMATCH -> getString(R.string.error_ssl_id_mismatch)
                SslError.SSL_NOTYETVALID -> getString(R.string.error_ssl_not_yet_valid)
                SslError.SSL_DATE_INVALID -> getString(R.string.error_ssl_date_invalid)
                else -> getString(R.string.error_ssl)
            }
            MaterialAlertDialogBuilder(context).setTitle(getString(R.string.error_ssl_title))
                .setMessage(errorMessage)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                    handler?.cancel()
                }.setPositiveButton(resources.getString(R.string.proceed)) { _, _ ->
                    handler?.proceed()
                }.setCancelable(false).show()
        }

        @UiThread
        override fun onReceivedHttpError(
            view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse
        ) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.RECEIVE_HTTP_ERROR)) {
                super.onReceivedHttpError(view, request, errorResponse)
            }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            onBackPressedCallback.isEnabled = webView.canGoBack()
        }

        override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
            super.onFormResubmission(view, dontResend, resend)
        }
    }

    inner class CustomWebChromeClient : WebChromeClient() {

        private var customView: View? = null
        private var defaultOrientation = 0
        private var customViewCallback: CustomViewCallback? = null
        private var pathCallback: ValueCallback<Array<Uri>>? = null

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            progressBar.progress = newProgress
            if (newProgress == 100) progressBar.hideView()
        }

        override fun onCreateWindow(
            view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
        ): Boolean {
            if (!isUserGesture && enablePopupBlocker) {
                Snackbar.make(
                    binding.contextView, getString(R.string.popup_blocked), Snackbar.LENGTH_LONG
                ).show()
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
            if (title != null && !URLUtil.isNetworkUrl(title) && title != "Webpage not available") toolbar.title =
                title
        }

        override fun onJsAlert(
            view: WebView?, url: String?, message: String?, result: JsResult?
        ): Boolean {
            val host = Uri.parse(webView.url).host
            MaterialAlertDialogBuilder(context).setTitle(getString(R.string.js_dialog_title, host))
                .setMessage(message).setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    result?.confirm()
                }.setOnCancelListener {
                    result?.cancel()
                }.show()
            return true
        }

        override fun onJsConfirm(
            view: WebView?, url: String?, message: String?, result: JsResult?
        ): Boolean {
            val host = Uri.parse(webView.url).host
            MaterialAlertDialogBuilder(context).setTitle(getString(R.string.js_dialog_title, host))
                .setMessage(message)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                    result?.cancel()
                }.setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    result?.confirm()
                }.setOnCancelListener {
                    result?.cancel()
                }.show()
            return true
        }

        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            val host = Uri.parse(webView.url).host
            val inputLayout: View = layoutInflater.inflate(R.layout.view_edittext, null)
            val textInputEditText: TextInputEditText = inputLayout.findViewById(R.id.input_text)
            textInputEditText.maxLines = 1
            textInputEditText.setText(defaultValue)
            MaterialAlertDialogBuilder(context).setTitle(getString(R.string.js_dialog_title, host))
                .setMessage(message).setView(inputLayout)
                .setNegativeButton(resources.getString(R.string.cancel)) { _, _ ->
                    result?.cancel()
                }.setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    if (textInputEditText.text.isNullOrEmpty()) {
                        result?.confirm()
                    } else {
                        result?.confirm(textInputEditText.text.toString())
                    }
                }.setOnCancelListener {
                    result?.cancel()
                }.show()
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
                            MaterialAlertDialogBuilder(context).setMessage(
                                getString(
                                    R.string.access_message_camera_web,
                                    urlHost
                                )
                            ).setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                                onPermissionRequestConfirmation(false, arrayOf(""))
                            }.setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                                requestWebViewPermission(
                                    Manifest.permission.CAMERA,
                                    CAMERA_PERMISSION_REQUEST_CODE,
                                    getString(R.string.request_message_permission_camera_web),
                                    arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                                )
                            }.setOnCancelListener {
                                onPermissionRequestConfirmation(false, arrayOf(""))
                            }.show()
                        }
                        PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                            MaterialAlertDialogBuilder(context).setMessage(
                                getString(
                                    R.string.access_message_mic_web,
                                    urlHost
                                )
                            ).setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                                onPermissionRequestConfirmation(false, arrayOf(""))
                            }.setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                                requestWebViewPermission(
                                    Manifest.permission.RECORD_AUDIO,
                                    MIC_PERMISSION_REQUEST_CODE,
                                    getString(R.string.request_message_permission_mic_web),
                                    arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                                )
                            }.setOnCancelListener {
                                onPermissionRequestConfirmation(false, arrayOf(""))
                            }.show()
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

        override fun onGeolocationPermissionsShowPrompt(
            origin: String?, callback: GeolocationPermissions.Callback?
        ) {
            geolocationCallback = null
            geolocationOrigin = null
            geolocationCallback = callback
            geolocationOrigin = origin
            MaterialAlertDialogBuilder(context).setMessage(
                getString(
                    R.string.access_message_location_web,
                    urlHost
                )
            ).setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                onGeolocationPermissionConfirmation(
                    geolocationOrigin, allowed = false, retain = false
                )
            }.setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                requestLocationPermission()
            }.setOnCancelListener {
                onGeolocationPermissionConfirmation(
                    geolocationOrigin, allowed = false, retain = false
                )
            }.show()
        }

        override fun onGeolocationPermissionsHidePrompt() {
            Log.d(TAG, "Geolocation permission prompt hide")
            geolocationCallback = null
            geolocationOrigin = null
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            if (customView != null) {
                onHideCustomView()
                return
            }
            view?.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
            customView = view
            defaultOrientation = requestedOrientation
            customViewCallback = callback
            (window.decorView as FrameLayout).addView(customView, FrameLayout.LayoutParams(-1, -1))
            controlSystemUi(true)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(customView)
            controlSystemUi(false)
            requestedOrientation = defaultOrientation
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        }

        override fun getDefaultVideoPoster(): Bitmap? {
            return if (super.getDefaultVideoPoster() == null) {
                BitmapFactory.decodeResource(resources, R.drawable.img_poster_horizontal)
            } else super.getDefaultVideoPoster()
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            if (fileChooserParams != null) {
                if (pathCallback != null) {
                    pathCallback?.onReceiveValue(null)
                    pathCallback = null
                }
                pathCallback = filePathCallback
                val allowMultiple = fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                val captureEnabled = fileChooserParams.isCaptureEnabled

                val chooserIntent: Intent = fileChooserParams.createIntent().apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    if (allowMultiple) putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    if (captureEnabled) {
                        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, setImageUri())
                        }
                        putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent))
                    }
                }
                try {
                    activityResultLauncher.launch(chooserIntent)
                } catch (exception: ActivityNotFoundException) {
                    pathCallback?.onReceiveValue(null)
                    pathCallback = null
                    Toast.makeText(context, R.string.error_default, Toast.LENGTH_LONG).show()
                }
            }
            return true
        }

        // User defined functions

        private fun controlSystemUi(hide: Boolean) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, webView).let { controller ->
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (hide) {
                    controller.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        private val activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                var uris: Array<Uri>? = null

                if (result.resultCode == Activity.RESULT_OK) {
                    val clipData = result.data?.clipData
                    val fileCount = clipData?.itemCount

                    if (clipData != null && fileCount != null) {
                        val uriList: MutableList<Uri> = mutableListOf()

                        for (i in 0 until fileCount) {
                            val uri = clipData.getItemAt(i)?.uri
                            if (uri != null) uriList.add(uri)
                        }
                        uris = uriList.toTypedArray()
                    } else {
                        result.data?.dataString?.also { uri ->
                            uris = arrayOf(Uri.parse(uri))
                        }
                    }
                }
                pathCallback?.onReceiveValue(uris)
                pathCallback = null
            }

        private fun setImageUri(): Uri? {
            val folder = File("${getExternalFilesDir(Environment.DIRECTORY_DCIM)}")
            if (!folder.exists()) folder.mkdirs()

            val file = File(folder, "${abs(Random().nextLong())}.png").apply {
                createNewFile()
                deleteOnExit()
            }

            return FileProvider.getUriForFile(
                context, getString(R.string.file_provider_authority), file
            )
        }
    }

    private fun overrideUrlLoading(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        if (URLUtil.isNetworkUrl(url)) {
            return false
        } else {
            when {
                url.startsWith(INTENT_SCHEME) -> {
                    try {
                        val handlerIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        if (handlerIntent != null) {
                            val info = packageManager.resolveActivity(
                                handlerIntent, PackageManager.MATCH_DEFAULT_ONLY
                            )
                            if (info != null) {
                                startActivity(handlerIntent)
                            } else {
                                val marketIntent = Intent(Intent.ACTION_VIEW)
                                marketIntent.data =
                                    Uri.parse("market://details?id=" + handlerIntent.getPackage())
                                try {
                                    startActivity(marketIntent)
                                } catch (notFoundException: ActivityNotFoundException) {
                                    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                                    if (fallbackUrl != null) {
                                        val intent =
                                            Intent(context, BrowserActivity::class.java).apply {
                                                putExtra("WEB_URL", fallbackUrl)
                                            }
                                        startActivity(intent)
                                    } else Toast.makeText(
                                        context,
                                        getString(R.string.error_url_load_fail),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    } catch (uriSyntaxException: URISyntaxException) {
                        Toast.makeText(
                            context, getString(R.string.error_url_load_fail), Toast.LENGTH_LONG
                        ).show()
                    }
                }
                else -> {
                    try {
                        val unknownURLIntent = Intent(Intent.ACTION_VIEW)
                        unknownURLIntent.data = Uri.parse(url)
                        startActivity(unknownURLIntent)
                    } catch (e: Exception) {
                        if (url.startsWith(TORRENT_SCHEME)) {
                            Toast.makeText(
                                context,
                                getString(R.string.error_no_torrent_client),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                getString(R.string.error_unsupported_url),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
        return true
    }

    private fun onReceivedError() {
        webView.evaluateJavascript(
            "javascript:document.open();document.write('');document.close();", null
        )
        webView.hideView()
        binding.errorLayout.showView()
    }

    private fun onReload() {
        binding.errorLayout.hideView()
        // Using timer to avoid multiple clicking on retry
        handler = Handler(Looper.getMainLooper())
        handler?.postDelayed({
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

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestWebViewPermission(
        permission: String, requestCode: Int, message: String, requestResources: Array<String>
    ) {
        val iconId = when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> R.drawable.ic_baseline_camera_24
            else -> R.drawable.ic_baseline_mic_24
        }
        when {
            isPermissionGranted(permission) -> {
                Log.d(TAG, "Permission already granted")
                onPermissionRequestConfirmation(true, requestResources)
            }
            shouldShowRequestPermissionRationale(permission) -> {
                MaterialAlertDialogBuilder(
                    context,
                    R.style.ThemeOverlay_YSports_MaterialAlertDialog_Centered_FullWidthButtons
                ).setTitle(getString(R.string.request_title_allow_permission)).setIcon(iconId)
                    .setMessage(message)
                    .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                        onPermissionRequestConfirmation(false, arrayOf(""))
                    }.setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                        requestPermissions(arrayOf(permission), requestCode)
                    }.setCancelable(false).show()
            }
            else -> {
                requestPermissions(arrayOf(permission), requestCode)
            }
        }
    }

    private fun onPermissionRequestConfirmation(allowed: Boolean, resources: Array<String>) {
        if (permissionRequest != null) {
            if (allowed) {
                permissionRequest?.grant(resources)
                Log.d(TAG, "Permission granted")
            } else {
                permissionRequest?.deny()
                Log.e(TAG, "Permission request denied")
            }
            permissionRequest = null
        }
    }

    private fun isLocationServiceEnabled(): Boolean {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun requestLocationPermission() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
        )
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
                MaterialAlertDialogBuilder(
                    context,
                    R.style.ThemeOverlay_YSports_MaterialAlertDialog_Centered_FullWidthButtons
                ).setTitle(getString(R.string.request_title_allow_permission))
                    .setIcon(R.drawable.ic_baseline_location_on_24)
                    .setMessage(getString(R.string.request_message_permission_location_web))
                    .setNegativeButton(resources.getString(R.string.block)) { _, _ ->
                        onGeolocationPermissionConfirmation(
                            geolocationOrigin, allowed = false, retain = false
                        )
                    }.setPositiveButton(resources.getString(R.string.allow)) { _, _ ->
                        requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE)
                    }.setCancelable(false).show()
            }
            else -> {
                requestPermissions(permissions, LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun onGeolocationPermissionConfirmation(
        origin: String?, allowed: Boolean, retain: Boolean
    ) {
        if (geolocationCallback != null) {
            geolocationCallback?.invoke(origin, allowed, retain)
            geolocationCallback = null
            geolocationOrigin = null
        }
    }

    private fun fetchLocation() {
        if (!isLocationServiceEnabled()) {
            onGeolocationPermissionConfirmation(geolocationOrigin, allowed = false, retain = false)
            Toast.makeText(
                context, getString(R.string.error_disabled_location_service), Toast.LENGTH_LONG
            ).show()
            val locationIntent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            try {
                startActivity(locationIntent)
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString(), e.cause)
            }
        } else {
            onGeolocationPermissionConfirmation(geolocationOrigin, allowed = true, retain = false)
        }
    }

    private fun startDownload(
        url: String, userAgent: String, mimetype: String, fileName: String
    ): Long {
        val downloadURI = Uri.parse(url)
        val request: DownloadManager.Request = DownloadManager.Request(downloadURI)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        request.setTitle(fileName)
        request.setDescription(getString(R.string.app_name))
        request.setMimeType(mimetype)
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader("cookie", cookies)
        request.addRequestHeader("User-Agent", userAgent)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        Snackbar.make(
            binding.contextView, getString(R.string.downloading_file), Snackbar.LENGTH_LONG
        ).show()
        return downloadManager.enqueue(request)
    }

    private fun startDownload(url: String): Long {
        val downloadURI = Uri.parse(url)
        val request: DownloadManager.Request = DownloadManager.Request(downloadURI)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setAllowedOverRoaming(false)
        request.setDescription(getString(R.string.app_name))
        val cookies = CookieManager.getInstance().getCookie(url)
        request.addRequestHeader("cookie", cookies)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        Snackbar.make(
            binding.contextView, getString(R.string.downloading_file), Snackbar.LENGTH_LONG
        ).show()
        return downloadManager.enqueue(request)
    }

    private val downloadReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadReference == referenceId) {
                Snackbar.make(
                    binding.contextView, getString(R.string.download_complete), Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun convertBlobToUri(blobURL: String, mimetype: String, fileName: String) {
        var script = assets.open("web/js/blob_converter.js").bufferedReader().use {
            it.readText()
        }
        script += "blobConvert('$blobURL', '$mimetype', '$fileName');"
        webView.evaluateJavascript(script, null)
    }

    private fun shareText(intentTitle: String?, subject: String?, text: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val chooser: Intent = Intent.createChooser(sendIntent, intentTitle)
        if (sendIntent.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        }
    }

    private fun shareImageFromUri(intentTitle: String?, uriToImage: Uri) {
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uriToImage)
            type = "image/*"
        }
        startActivity(Intent.createChooser(shareIntent, intentTitle))
    }

    private fun shareImageFromUrl(urlToImage: String) {
        Glide.with(context).asBitmap().load(urlToImage).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                //val path: String = Media.insertImage(contentResolver, resource, "${abs(random.nextLong())}", urlToImage)
                val path = saveImage(resource)
                if (path != null) shareImageFromUri(getString(R.string.share_via), path)
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }
        })
    }

    private fun saveImage(bitmap: Bitmap): Uri? {
        val path = File(cacheDir, "images")
        if (!path.exists()) path.mkdirs()
        val file = File(path, "${abs(Random().nextLong())}.png").also {
            FileOutputStream(it).use { fileOutputStream ->
                bitmap.compress(
                    Bitmap.CompressFormat.PNG, 100, fileOutputStream
                )
            }
        }.apply {
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            context, getString(R.string.file_provider_authority), file
        )
    }

    private fun showLongClickMenu(items: Array<String>, header: View): BottomSheetMenu {
        val bottomSheetMenu = BottomSheetMenu()
        for (item in items) {
            bottomSheetMenu.setMenuItem(item)
        }
        bottomSheetMenu.setHeaderView(header)
        return bottomSheetMenu
    }
}