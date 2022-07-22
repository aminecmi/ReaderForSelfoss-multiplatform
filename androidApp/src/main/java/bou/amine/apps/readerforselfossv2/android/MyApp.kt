package bou.amine.apps.readerforselfossv2.android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import bou.amine.apps.readerforselfossv2.DI.networkModule
import bou.amine.apps.readerforselfossv2.android.utils.Config
import bou.amine.apps.readerforselfossv2.android.utils.glide.loadMaybeBasicAuth
import bou.amine.apps.readerforselfossv2.repository.Repository
import bou.amine.apps.readerforselfossv2.repository.RepositoryImpl
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ftinc.scoop.Scoop
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.russhwolf.settings.Settings
import org.kodein.di.*
import java.util.UUID.randomUUID

class MyApp : MultiDexApplication(), DIAware {

    override val di by DI.lazy {
        import(networkModule)
        bind<Repository>() with singleton { RepositoryImpl(instance(), instance()) }
    }

    private lateinit var config: Config
    private lateinit var settings : Settings

    override fun onCreate() {
        super.onCreate()
        config = Config()
        settings = Settings()

        if (settings.getString("unique_id", "").isEmpty()) {
            settings.putString("unique_id", randomUUID().toString())
        }

        initDrawerImageLoader()

        initTheme()

        tryToHandleBug()

        handleNotificationChannels()
    }

    private fun handleNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val name = getString(R.string.notification_channel_sync)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(Config.syncChannelId, name, importance)

            val newItemsChannelname = getString(R.string.new_items_channel_sync)
            val newItemsChannelimportance = NotificationManager.IMPORTANCE_DEFAULT
            val newItemsChannelmChannel = NotificationChannel(Config.newItemsChannelId, newItemsChannelname, newItemsChannelimportance)

            notificationManager.createNotificationChannel(mChannel)
            notificationManager.createNotificationChannel(newItemsChannelmChannel)
        }
    }

    private fun initDrawerImageLoader() {
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                Glide.with(imageView.context)
                    .loadMaybeBasicAuth(config, uri.toString())
                    .apply(RequestOptions.fitCenterTransform().placeholder(placeholder))
                    .into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.with(imageView.context).clear(imageView)
            }

            override fun placeholder(ctx: Context, tag: String?): Drawable {
                return baseContext.resources.getDrawable(R.mipmap.ic_launcher)
            }
        })
    }

    private fun initTheme() {
        Scoop.waffleCone()
            .addFlavor(getString(R.string.default_theme), R.style.NoBar, true)
            .addFlavor(getString(R.string.default_dark_theme), R.style.NoBarDark, false)
            .setSharedPreferences(PreferenceManager.getDefaultSharedPreferences(this))
            .initialize()
    }

    private fun tryToHandleBug() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            if (e is java.lang.NoClassDefFoundError && e.stackTrace.asList().any {
                    it.toString().contains("android.view.ViewDebug")
                }) {
                Unit
            } else {
                oldHandler.uncaughtException(thread, e)
            }
        }
    }
}