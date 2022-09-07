package ysports.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import ysports.app.databinding.ActivitySettingsBinding
import ysports.app.util.FileUtil

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private val fileUtil = FileUtil()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val notificationsPreference: Preference? = findPreference("notifications")
            val themePreference: ListPreference? = findPreference("theme")
            val cachePreference: Preference? = findPreference("clear_cache")
            val aboutPreference: Preference? = findPreference("about")
            val licensesPreference: Preference? = findPreference("licenses")
            val versionPreference: Preference? = findPreference("version")

            when (themePreference?.value) {
                "light" -> themePreference.summary = getString(R.string.summary_theme_light)
                "dark" -> themePreference.summary = getString(R.string.summary_theme_dark)
                else -> themePreference?.summary = getString(R.string.summary_theme_system_default)
            }
            cachePreference?.summary = getCacheSize()
            versionPreference?.summary = getString(R.string.summary_version, BuildConfig.VERSION_NAME)

            notificationsPreference?.setOnPreferenceClickListener {
                val intent = Intent().apply {
                    action = "android.settings.APP_NOTIFICATION_SETTINGS"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        putExtra("android.provider.extra.APP_PACKAGE", context?.packageName)
                    } else {
                        putExtra("app_package", context?.packageName)
                        putExtra("app_uid", context?.applicationInfo?.uid)
                    }
                }
                startActivity(intent)
                true
            }

            themePreference?.setOnPreferenceChangeListener { preference, newValue ->
                when (newValue) {
                    "light" -> {
                        preference.summary = getString(R.string.summary_theme_light)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    "dark" -> {
                        preference.summary = getString(R.string.summary_theme_dark)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    else -> {
                        preference.summary = getString(R.string.summary_theme_system_default)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                        }
                    }
                }
                true
            }

            cachePreference?.setOnPreferenceClickListener {
                val cacheDeleted = fileUtil.deleteDir(context?.cacheDir)
                val externalCacheDeleted = fileUtil.deleteDir(context?.externalCacheDir)
                if (cacheDeleted || externalCacheDeleted) {
                    Toast.makeText(context, getString(R.string.cache_cleared), Toast.LENGTH_LONG).show()
                }
                it.summary = getCacheSize()
                true
            }

            aboutPreference?.setOnPreferenceClickListener {
                val intent = Intent(context, BrowserActivity::class.java).apply {
                    putExtra("WEB_URL", getString(R.string.url_about))
                }
                startActivity(intent)
                true
            }

            licensesPreference?.setOnPreferenceClickListener {
                startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                true
            }
        }

        private fun getCacheSize() : String {
            var size: Long = 0
            size += fileUtil.getDirSize(context?.cacheDir)
            size += fileUtil.getDirSize(context?.externalCacheDir)
            val cacheSize = if (size > 0) (size shr 20).toDouble() else 0
            return "$cacheSize MB"
        }
    }
}