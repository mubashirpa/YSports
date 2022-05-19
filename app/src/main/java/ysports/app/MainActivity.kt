package ysports.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import ysports.app.databinding.ActivityMainBinding
import ysports.app.player.PlayerUtil
import ysports.app.ui.fixtures.FixturesFragment
import ysports.app.ui.home.HomeFragment
import ysports.app.ui.more.MoreFragment
import ysports.app.util.AppUtil

@Suppress("PrivatePropertyName")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var context: Context
    private lateinit var toolbar: MaterialToolbar
    private val TAG: String = "MainActivity"
    private lateinit var drawerLayout: DrawerLayout
    private val playerUtil = PlayerUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this
        toolbar = binding.materialToolbar
        drawerLayout = binding.drawerLayout
        val bottomNavigation = binding.bottomNavigation

        val homeFragment = HomeFragment()
        val fixturesFragment = FixturesFragment()
        val moreFragment = MoreFragment()
        bottomNavigation.selectedItemId = R.id.navigation_home

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
            add(R.id.frame_layout, homeFragment)
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

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.network_stream -> {
                    val clipboardURL = getFromClipboard()
                    val inputLayout: View = layoutInflater.inflate(R.layout.view_input_view_dialog, null)
                    val textInputEditText: TextInputEditText = inputLayout.findViewById(R.id.input_text)
                    if (clipboardURL != null && URLUtil.isValidUrl(clipboardURL)) textInputEditText.setText(clipboardURL)
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Network Stream")
                        .setView(inputLayout)
                        .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                            val url = textInputEditText.text.toString()
                            if (url.isEmpty() || url.isBlank()) {
                                Toast.makeText(context, "Empty URL", Toast.LENGTH_LONG).show()
                                return@setPositiveButton
                            }
                            if (!URLUtil.isValidUrl(url)) {
                                Toast.makeText(context, "Invalid URL", Toast.LENGTH_LONG).show()
                                return@setPositiveButton
                            }
                            playerUtil.loadPlayer(context, Uri.parse(url), true)
                        }
                        .setNegativeButton(resources.getString(R.string.cancel)) { _, _ -> }
                        .show()
                }
            }
            drawerLayout.close()
            true
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> {
                    supportFragmentManager.beginTransaction().apply {
                        if (fixturesFragment.isAdded && fixturesFragment.isVisible) hide(fixturesFragment)
                        if (moreFragment.isAdded && moreFragment.isVisible) hide(moreFragment)
                        if (homeFragment.isAdded) show(homeFragment) else add(R.id.frame_layout, homeFragment)
                    }.commit()
                    true
                }
                R.id.navigation_fixtures -> {
                    supportFragmentManager.beginTransaction().apply {
                        if (homeFragment.isVisible) hide(homeFragment)
                        if (moreFragment.isAdded && moreFragment.isVisible) hide(moreFragment)
                        if (fixturesFragment.isAdded) show(fixturesFragment) else add(R.id.frame_layout, fixturesFragment)
                    }.commit()
                    true
                }
                R.id.navigation_more -> {
                    supportFragmentManager.beginTransaction().apply {
                        if (homeFragment.isVisible) hide(homeFragment)
                        if (fixturesFragment.isAdded && fixturesFragment.isVisible) hide(fixturesFragment)
                        if (moreFragment.isAdded) show(moreFragment) else add(R.id.frame_layout, moreFragment)
                    }.commit()
                    true
                }
                else -> false
            }
        }

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
        val channelId: String = getString(R.string.default_notification_channel_id)
        val channelName: CharSequence = getString(R.string.default_notification_channel_name)
        val channelDescription = getString(R.string.default_notification_channel_description)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = channelDescription
            channel.enableLights(true)
            channel.setShowBadge(true)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun handleIntent(intent: Intent?) {
        val appLinkAction: String? = intent?.action
        val appLinkData: Uri? = intent?.data
        if (appLinkAction == Intent.ACTION_VIEW && appLinkData != null) {
            if (appLinkData.path.toString() == "/play") {
                Log.d(TAG, "Loading player")
                try {
                    val videoUri = Uri.parse(appLinkData.getQueryParameter("url"))
                    Log.d(TAG, videoUri.toString())
                    playerUtil.loadPlayer(context, videoUri, true)
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
}