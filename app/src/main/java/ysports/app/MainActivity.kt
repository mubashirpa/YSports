package ysports.app

import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import ysports.app.databinding.ActivityMainBinding
import ysports.app.player.PlayerUtil
import ysports.app.ui.leagues.LeaguesFragment
import ysports.app.ui.matches.MatchesFragment
import ysports.app.ui.more.MoreFragment
import ysports.app.ui.news.NewsFragment
import ysports.app.util.AppUtil
import ysports.app.util.NotificationUtil

@Suppress("PrivatePropertyName")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var context: Context
    private lateinit var toolbar: MaterialToolbar
    private val TAG: String = "MainActivity"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationDrawer: NavigationView
    private var navigationBar: BottomNavigationView? = null
    private var navigationRail: NavigationRailView? = null
    private val playerUtil = PlayerUtil()
    private val leaguesFragment = LeaguesFragment()
    private val matchesFragment = MatchesFragment()
    private val newsFragment = NewsFragment()
    private val moreFragment = MoreFragment()
    private var tablet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        toolbar = binding.materialToolbar
        drawerLayout = binding.drawerLayout
        navigationDrawer = binding.navigationView
        navigationBar = binding.bottomNavigation
        navigationRail = binding.navigationRail

        // Removes all the available fragments (in case of app restart)
        supportFragmentManager.fragments.let {
            if (it.isNotEmpty()) {
                supportFragmentManager.beginTransaction().apply {
                    for (fragment in it) {
                        remove(fragment)
                    }
                    commit()
                }
            }
        }

        supportFragmentManager.beginTransaction().apply {
            add(R.id.frame_layout, matchesFragment)
        }.commit()

        toolbar.setNavigationOnClickListener {
            drawerLayout.open()
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.search -> {
                    // TODO("Handle search icon press")
                    val intent = Intent(context, BrowserActivity::class.java).apply {
                        putExtra("WEB_URL", "https://google.com")
                    }
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.matches_item -> {
                    changeFragmentDestination(0)
                }
                R.id.leagues_item -> {
                    changeFragmentDestination(1)
                }
                R.id.news_item -> {
                    changeFragmentDestination(2)
                }
                R.id.more_item -> {
                    changeFragmentDestination(3)
                }
                R.id.network_stream_item -> {
                    networkStream()
                }
            }
            drawerLayout.close()
            true
        }

        navigationBar?.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.matches -> {
                    changeFragmentDestination(0)
                    true
                }
                R.id.leagues -> {
                    changeFragmentDestination(1)
                    true
                }
                R.id.news -> {
                    changeFragmentDestination(2)
                    true
                }
                R.id.more -> {
                    changeFragmentDestination(3)
                    true
                }
                else -> false
            }
        }

        navigationRail?.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.matches -> {
                    changeFragmentDestination(0)
                    true
                }
                R.id.leagues -> {
                    changeFragmentDestination(1)
                    true
                }
                R.id.news -> {
                    changeFragmentDestination(2)
                    true
                }
                R.id.more -> {
                    changeFragmentDestination(3)
                    true
                }
                else -> false
            }
        }

        tablet = isTablet()
        handleIntent(intent)
        checkUpdate()
        createNotificationChannel()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(binding.navigationView)) {
            drawerLayout.close()
        } else {
            super.onBackPressed()
            finish()
        }
    }

    private fun checkUpdate() {
        val currentVersion: String = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (nameNotFoundException: PackageManager.NameNotFoundException) {
            return
        }
        val database = Firebase.database
        val reference = database.getReference("version")
        reference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ind: GenericTypeIndicator<HashMap<String, Any>> = object : GenericTypeIndicator<HashMap<String, Any>>() {}
                val childKey: String? = snapshot.key
                val childValue: HashMap<String, Any>? = snapshot.getValue(ind)
                if (childKey == null) {
                    return
                }
                if (childKey == "version") {
                    if (childValue != null && childValue.containsKey("latest")) {
                        val latestVersion: String = childValue["latest"].toString()
                        if (currentVersion != latestVersion) {
                            Toast.makeText(context, "Update available", Toast.LENGTH_LONG).show()
                            if (childValue.containsKey("url")) AppUtil(context).openCustomTabs(childValue["url"].toString())
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, getString(R.string.error_fdb_on_cancelled))
            }

        })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId: String = getString(R.string.default_notification_channel_id)
            val channelName: String = getString(R.string.default_notification_channel_name)
            val channelDescription = getString(R.string.default_notification_channel_description)
            NotificationUtil(context).createNotificationChannel(channelName, channelDescription, channelId, NotificationManager.IMPORTANCE_DEFAULT)
        }
    }

    private fun handleIntent(intent: Intent?) {
        // TODO("Handle exo player intent")
        val appLinkAction: String? = intent?.action
        val appLinkData: Uri? = intent?.data
        if (appLinkAction == Intent.ACTION_VIEW && appLinkData != null) {
            if (appLinkData.path.toString() == "/play") {
                Log.d(TAG, "Loading player")
                try {
                    val videoUri = Uri.parse(appLinkData.getQueryParameter("url"))
                    playerUtil.loadPlayer(context, videoUri, null, true)
                } catch (e: NullPointerException) {
                    Log.d(TAG, "Failed to load player")
                }
            }
        }
    }

    private fun getFromClipboard(): String? {
        val clipboardManager: ClipboardManager =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        return if (clipboardManager.primaryClip != null) {
            clipboardManager.primaryClip!!.getItemAt(0).coerceToText(context).toString()
        } else null
    }

    private fun hideFragments() {
        supportFragmentManager.fragments.let {
            if (it.isNotEmpty()) {
                supportFragmentManager.beginTransaction().apply {
                    for (fragment in it) {
                        hide(fragment)
                    }
                    commit()
                }
            }
        }
    }

    private fun changeFragmentDestination(destination: Int) {
        when (destination) {
            0 -> {
                hideFragments()
                supportFragmentManager.beginTransaction().apply {
                    if (matchesFragment.isAdded) show(matchesFragment) else add(R.id.frame_layout, matchesFragment)
                }.commit()
            }
            1 -> {
                hideFragments()
                supportFragmentManager.beginTransaction().apply {
                    if (leaguesFragment.isAdded) show(leaguesFragment) else add(R.id.frame_layout, leaguesFragment)
                }.commit()
            }
            2 -> {
                hideFragments()
                supportFragmentManager.beginTransaction().apply {
                    if (newsFragment.isAdded) show(newsFragment) else add(R.id.frame_layout, newsFragment)
                }.commit()
            }
            3 -> {
                hideFragments()
                supportFragmentManager.beginTransaction().apply {
                    if (moreFragment.isAdded) show(moreFragment) else add(R.id.frame_layout, moreFragment)
                }.commit()
            }
        }
        setNavigationSelectedItem(destination)
    }

    private fun setNavigationSelectedItem(selectedItem: Int) {
        if (navigationDrawer.menu.size() == 1) return
        val drawerMenuItem = navigationDrawer.menu.getItem(0)
        if (tablet && drawerMenuItem.hasSubMenu()) {
            val subMenu = drawerMenuItem.subMenu
            for (i in 0..subMenu.size()) subMenu.getItem(i).isChecked = false
            subMenu.getItem(selectedItem).isChecked = true
        }
        navigationRail?.menu?.getItem(selectedItem)?.isChecked = true
    }

    private fun networkStream() {
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
        val clipboardURL = getFromClipboard()
        val inputLayout: View = layoutInflater.inflate(R.layout.view_input_view_dialog, null)
        val textInputLayout: TextInputLayout = inputLayout.findViewById(R.id.input_layout)
        val textInputEditText: TextInputEditText = inputLayout.findViewById(R.id.input_text)
        textInputLayout.hint = resources.getString(R.string.network_url)
        textInputLayout.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
        textInputEditText.imeOptions = EditorInfo.IME_ACTION_GO
        textInputEditText.inputType = InputType.TYPE_TEXT_VARIATION_URI

        textInputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val url = textInputEditText.text.toString()
                // TODO("Dismiss dialog")
                loadPlayer(url)
            }
            return@setOnEditorActionListener false
        }

        if (clipboardURL != null && URLUtil.isValidUrl(clipboardURL)) textInputEditText.setText(clipboardURL)
        materialAlertDialogBuilder
            .setTitle("Network Stream")
            .setMessage("Please enter a network URL:")
            .setView(inputLayout)
            .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                val url = textInputEditText.text.toString()
                loadPlayer(url)
            }
            .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
        materialAlertDialogBuilder.create().show()
    }

    private fun loadPlayer(url: String) {
        if (url.isEmpty() || url.isBlank()) {
            Toast.makeText(context, "Empty URL", Toast.LENGTH_LONG).show()
            return
        }
        if (!URLUtil.isValidUrl(url)) {
            Toast.makeText(context, "Invalid URL", Toast.LENGTH_LONG).show()
            return
        }
        playerUtil.loadPlayer(context, Uri.parse(url), null, true)
    }

    private fun isTablet() : Boolean {
        val widthDp = resources.displayMetrics.run { widthPixels / density }
        Log.d("NewsFragment", widthDp.toString())
        return widthDp >= 600
    }
}