package bou.amine.apps.readerforselfossv2.android.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.*
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import bou.amine.apps.readerforselfossv2.android.R
import bou.amine.apps.readerforselfossv2.android.databinding.ActivitySettingsBinding
import bou.amine.apps.readerforselfossv2.android.themes.Toppings
import bou.amine.apps.readerforselfossv2.android.utils.Config
import com.ftinc.scoop.Scoop
import com.russhwolf.settings.Settings
import java.lang.NumberFormatException

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AppCompatActivity(),
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_theme", false)) {
            setTheme(R.style.NoBarDark)
        }
        val binding = ActivitySettingsBinding.inflate(layoutInflater)

        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, binding.toolbar)
        scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)

        setContentView(binding.root)
        if (savedInstanceState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings, MainPreferenceFragment())
                    .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.title_activity_settings)
            }
        }

        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = title
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            supportActionBar?.title = getText(R.string.title_activity_settings)
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
            caller: PreferenceFragmentCompat,
            pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit()
        title = pref.title
        supportActionBar?.title = title
        return true
    }

    class MainPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_main, rootKey)
        }
    }

    class GeneralPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_general, rootKey)

            val editTextPreference = preferenceManager.findPreference<EditTextPreference>("prefer_api_items_number")
            editTextPreference?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.filters = arrayOf(
                        InputFilter { source, _, _, dest, _, _ ->
                            try {
                                val input: Int = (dest.toString() + source.toString()).toInt()
                                if (input in 1..200) return@InputFilter null
                            } catch (nfe: NumberFormatException) {
                                Toast.makeText(activity, R.string.items_number_should_be_number, Toast.LENGTH_LONG).show()
                            }
                            ""
                        }
                )
            }
        }
    }

    class ArticleViewerPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_viewer, rootKey)

            val fontSize = preferenceManager.findPreference<EditTextPreference>("reader_font_size")
            fontSize?.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_NUMBER
                editText.addTextChangedListener { object : TextWatcher {
                    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                    override fun afterTextChanged(editable: Editable) {
                        try {
                            editText.textSize = editable.toString().toInt().toFloat()
                        } catch (e: NumberFormatException) {
                        }
                    }
                } }
                editText.filters = arrayOf(
                        InputFilter { source, _, _, dest, _, _ ->
                            try {
                                val input = (dest.toString() + source.toString()).toInt()
                                if (input > 0) return@InputFilter null
                            } catch (nfe: NumberFormatException) {
                            }
                            ""
                        }
                )
            }
        }
    }

    class OfflinePreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_offline, rootKey)
        }
    }

    class ThemePreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_theme, rootKey)
            setHasOptionsMenu(true)
        }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)
            inflater.inflate(R.menu.settings_theme, menu)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == R.id.clear) {
                val settings = Settings()
                settings.remove("color_primary")
                settings.remove("color_primary_dark")
                settings.remove("color_accent")
                settings.remove("color_accent_dark")
                settings.remove("dark_theme")
                requireActivity().recreate()
            }
            return super.onOptionsItemSelected(item)
        }
    }

    class LinksPreferenceFragment : PreferenceFragmentCompat() {
        private fun openUrl(uri: Uri?) {
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(browserIntent)
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_links, rootKey)

            preferenceManager.findPreference<Preference>("trackerLink")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                openUrl(Uri.parse(Config.trackerUrl))
                true
            }

            preferenceManager.findPreference<Preference>("sourceLink")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                openUrl(Uri.parse(Config.sourceUrl))
                false
            }

            preferenceManager.findPreference<Preference>("translation")?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                openUrl(Uri.parse(Config.translationUrl))
                false
            }
        }
    }

    class ExperimentalPreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.pref_experimental, rootKey)
        }
    }
}