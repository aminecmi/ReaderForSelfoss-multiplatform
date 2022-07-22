package bou.amine.apps.readerforselfossv2.android.utils.glide

import android.content.Context
import bou.amine.apps.readerforselfossv2.android.utils.getUnsafeHttpClient
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.GlideModule
import com.russhwolf.settings.Settings
import java.io.InputStream

class SelfSignedGlideModule : GlideModule {

    override fun applyOptions(context: Context?, builder: GlideBuilder?) {
    }

    override fun registerComponents(context: Context?, glide: Glide?, registry: Registry?) {

        if (context != null) {
            val settings = Settings()
            if (settings.getBoolean("isSelfSignedCert", false)) {
                val client = getUnsafeHttpClient().build()

                registry?.append(
                    GlideUrl::class.java,
                    InputStream::class.java,
                    com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader.Factory(client)
                )
            }
        }
    }
}