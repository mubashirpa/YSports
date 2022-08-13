package ysports.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import ysports.app.databinding.ActivitySettingsBinding

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
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val notificationsPreference: Preference? = findPreference("notifications")
            val themePreference: ListPreference? = findPreference("theme")

            when (themePreference?.value) {
                "light" -> themePreference.summary = getString(R.string.theme_light_summary)
                "dark" -> themePreference.summary = getString(R.string.theme_dark_summary)
                else -> themePreference?.summary = getString(R.string.theme_system_default_summary)
            }

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
                        preference.summary = getString(R.string.theme_light_summary)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                    "dark" -> {
                        preference.summary = getString(R.string.theme_dark_summary)
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                    else -> {
                        preference.summary = getString(R.string.theme_system_default_summary)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        } else {
                            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                        }
                    }
                }
                true
            }
        }
    }
}