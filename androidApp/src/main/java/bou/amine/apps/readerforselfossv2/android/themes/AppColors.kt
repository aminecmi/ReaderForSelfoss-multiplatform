package bou.amine.apps.readerforselfossv2.android.themes

import android.app.Activity
import androidx.annotation.ColorInt
import bou.amine.apps.readerforselfossv2.android.R
import com.russhwolf.settings.Settings

class AppColors(a: Activity) {

    @ColorInt val colorPrimary: Int
    @ColorInt val colorPrimaryDark: Int
    @ColorInt val colorAccent: Int
    @ColorInt val colorAccentDark: Int
    @ColorInt val colorBackground: Int
    @ColorInt val textColor: Int
    val isDarkTheme: Boolean

    init {
        val settings = Settings()

        colorPrimary =
                settings.getInt(
                    "color_primary",
                    a.resources.getColor(R.color.colorPrimary)
                )
        colorPrimaryDark =
                settings.getInt(
                    "color_primary_dark",
                    a.resources.getColor(R.color.colorPrimaryDark)
                )
        colorAccent =
                settings.getInt(
                    "color_accent",
                    a.resources.getColor(R.color.colorAccent)
                )
        colorAccentDark =
                settings.getInt(
                    "color_accent_dark",
                    a.resources.getColor(R.color.colorAccentDark)
                )
        isDarkTheme =
                settings.getBoolean(
                    "dark_theme",
                    false
                )

        colorBackground = if (isDarkTheme) {
            a.setTheme(R.style.NoBarDark)
            R.color.darkBackground
        } else {
            a.setTheme(R.style.NoBar)
            R.color.grey_50
        }

        textColor = if (isDarkTheme) {
            R.color.white
        } else {
            R.color.grey_900
        }
    }
}
