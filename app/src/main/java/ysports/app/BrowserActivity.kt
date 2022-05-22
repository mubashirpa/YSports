package ysports.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.webkit.*
import androidx.webkit.WebSettingsCompat.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import ysports.app.databinding.ActivityBrowserBinding
import ysports.app.util.AdBlocker
import ysports.app.util.AppUtil

@Suppress("PrivatePropertyName")
class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private lateinit var context: Context
    private lateinit var WEB_URL: String
    private var safeBrowsingIsInitialized: Boolean = false
    private lateinit var webView: WebView
    private lateinit var toolbar: MaterialToolbar
    private val TAG = "BrowserActivity"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        toolbar = binding.materialToolbar
        webView = binding.webView
        AdBlocker.init(context)
        WEB_URL = intent.getStringExtra("WEB_URL") ?: "https://appassets.androidplatform.net/assets/web/error_404/index.html"

        toolbar.subtitle = Uri.parse(WEB_URL).host

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

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
            .build()

        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews for reference
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(false)
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(this) { success ->
                if (!success) {
                    Log.e(TAG, "Unable to initialize Safe Browsing!")
                    return@startSafeBrowsing
                }
                safeBrowsingIsInitialized = true
            }
        }

        // Supporting Dark Theme for WebView
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> {
                    setForceDark(webView.settings, FORCE_DARK_ON)
                }
                else -> {
                    setForceDark(webView.settings, FORCE_DARK_OFF)
                }
            }
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
            setForceDarkStrategy(webView.settings, DARK_STRATEGY_WEB_THEME_DARKENING_ONLY)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                offscreenPreRaster = true
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = true
            }
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            useWideViewPort = true
            userAgentString = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Mobile Safari/537.36"
        }
        webView.isSoundEffectsEnabled = true
        // Renderer Importance API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(RENDERER_PRIORITY_BOUND, true)
        }
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.webViewClient = CustomWebViewClient(assetLoader)
        webView.webChromeClient = CustomWebChromeClient()
        webView.loadUrl(WEB_URL)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else finish()
    }

    override fun onDestroy() {
        super.onDestroy()
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

    inner class WebAppInterface {

        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(context, toast, Toast.LENGTH_SHORT).show()
        }
    }

    inner class CustomWebViewClient(private val assetLoader: WebViewAssetLoader) : WebViewClientCompat() {

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        override fun onSafeBrowsingHit(view: WebView, request: WebResourceRequest, threatType: Int, callback: SafeBrowsingResponseCompat) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY)) {
                callback.backToSafety(true)
                Toast.makeText(view.context, "Unsafe web page blocked.", Toast.LENGTH_LONG).show()
                // TODO("Block page only if user wants")
            }
        }

        // Termination Handling API
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            if (detail != null && !detail.didCrash()) {
                // Renderer was killed because the system ran out of memory.
                // The app can recover gracefully by creating a new WebView instance in the foreground.
                Log.e(TAG, "System killed the WebView rendering process to reclaim memory. Recreating...")

                webView.also {
                    it.destroy()
                }
                return true // The app continues executing.
            }

            // Renderer crashed because of an internal error, such as a memory access violation.
            Log.e(TAG, "The WebView rendering process crashed!")
            return false
        }

        @Nullable
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            // return assetLoader.shouldInterceptRequest(request.url)
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
    }

    inner class CustomWebChromeClient : WebChromeClient() {

        private var hasShownCustomView: Boolean = false
        private var customView: View? = null
        private var defaultOrientation = 0
        private var defaultSystemUiVisibility = 0
        private var customViewCallback: CustomViewCallback? = null
        private var uploadMessage: ValueCallback<Array<Uri>>? = null

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.progress = newProgress
            if (newProgress == 0) binding.progressBar.visibility = View.VISIBLE
            if (newProgress == 100) binding.progressBar.visibility = View.GONE
        }

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            if (!isUserGesture) {
                Snackbar.make(binding.contextView, "Pop-up blocked", Snackbar.LENGTH_LONG).show()
                return false
            }
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            if (!title.isNullOrEmpty()) toolbar.title = title
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
            if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(null)
                uploadMessage = null
            }
            uploadMessage = filePathCallback
            val chooserIntent: Intent = fileChooserParams.createIntent()
            chooserIntent.addCategory(Intent.CATEGORY_OPENABLE)
            if (allowMultiple) chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            activityResultLauncher.launch(chooserIntent)
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
                window.setDecorFitsSystemWindows(false)
                val insetsController = window.insetsController
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    insetsController.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {

                // Deprecated in Api level 30
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

            }
        }

        private fun showSystemUi(defaultSystemUiVisibility: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(true)
                val insetsController = window.insetsController
                if (insetsController != null) {
                    insetsController.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    insetsController.systemBarsBehavior = defaultSystemUiVisibility
                }
            } else {

                //Deprecated in Api level 30
                window.decorView.systemUiVisibility = defaultSystemUiVisibility

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
            if (result.resultCode == Activity.RESULT_OK) {
                if (uploadMessage == null) return@registerForActivityResult
                uploadMessage!!.onReceiveValue(FileChooserParams.parseResult(result.resultCode, result.data))
            } else {
                uploadMessage?.onReceiveValue(null)
            }
            uploadMessage = null
        }
    }
}