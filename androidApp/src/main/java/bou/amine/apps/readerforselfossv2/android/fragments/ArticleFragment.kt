package bou.amine.apps.readerforselfossv2.android.fragments

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.room.Room
import bou.amine.apps.readerforselfossv2.android.ImageActivity
import bou.amine.apps.readerforselfossv2.android.R
import bou.amine.apps.readerforselfossv2.android.api.mercury.MercuryApi
import bou.amine.apps.readerforselfossv2.android.api.mercury.ParsedContent
import bou.amine.apps.readerforselfossv2.android.databinding.FragmentArticleBinding
import bou.amine.apps.readerforselfossv2.android.model.*
import bou.amine.apps.readerforselfossv2.android.persistence.database.AppDatabase
import bou.amine.apps.readerforselfossv2.android.persistence.migrations.MIGRATION_1_2
import bou.amine.apps.readerforselfossv2.android.persistence.migrations.MIGRATION_2_3
import bou.amine.apps.readerforselfossv2.android.persistence.migrations.MIGRATION_3_4
import bou.amine.apps.readerforselfossv2.android.themes.AppColors
import bou.amine.apps.readerforselfossv2.android.utils.*
import bou.amine.apps.readerforselfossv2.android.utils.customtabs.CustomTabActivityHelper
import bou.amine.apps.readerforselfossv2.android.utils.glide.getBitmapInputStream
import bou.amine.apps.readerforselfossv2.android.utils.glide.loadMaybeBasicAuth
import bou.amine.apps.readerforselfossv2.android.utils.network.isNetworkAvailable
import bou.amine.apps.readerforselfossv2.repository.Repository
import bou.amine.apps.readerforselfossv2.rest.SelfossModel
import bou.amine.apps.readerforselfossv2.utils.DateUtils
import bou.amine.apps.readerforselfossv2.utils.isEmptyOrNullOrNullString
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.x.closestDI
import org.kodein.di.instance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException

class ArticleFragment : Fragment(), DIAware {
    private var fontSize: Int = 16
    private lateinit var item: SelfossModel.Item
    private var mCustomTabActivityHelper: CustomTabActivityHelper? = null
    private lateinit var url: String
    private lateinit var contentText: String
    private lateinit var contentSource: String
    private lateinit var contentImage: String
    private lateinit var contentTitle: String
    private lateinit var allImages : ArrayList<String>
    private lateinit var fab: FloatingActionButton
    private lateinit var appColors: AppColors
    private lateinit var db: AppDatabase
    private lateinit var textAlignment: String
    private lateinit var config: Config
    private var _binding: FragmentArticleBinding? = null
    private val binding get() = _binding!!

    override val di : DI by closestDI()
    private val repository: Repository by instance()

    private var settings = Settings()

    private var typeface: Typeface? = null
    private var resId: Int = 0
    private var font = ""
    private var staticBar = false

    override fun onStop() {
        super.onStop()
        if (mCustomTabActivityHelper != null) {
            mCustomTabActivityHelper!!.unbindCustomTabsService(activity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(requireActivity())
        config = Config()

        super.onCreate(savedInstanceState)

        val pi: ParecelableItem = requireArguments().getParcelable(ARG_ITEMS)!!

        item = pi.toModel()

        db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "selfoss-database"
        ).addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3).addMigrations(MIGRATION_3_4).build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentArticleBinding.inflate(inflater, container, false)

            url = item.getLinkDecoded()
            contentText = item.content
            contentTitle = item.getTitleDecoded()
            contentImage = item.getThumbnail(repository.baseUrl)
            contentSource = item.sourceAndDateText(DateUtils(repository.apiMajorVersion))
            allImages = item.getImages()

            fontSize = settings.getString("reader_font_size", "16").toInt()
            staticBar = settings.getBoolean("reader_static_bar", false)

            font = settings.getString("reader_font", "")
            if (font.isNotEmpty()) {
                resId = requireContext().resources.getIdentifier(font, "font", requireContext().packageName)
                typeface = try {
                    ResourcesCompat.getFont(requireContext(), resId)!!
                } catch (e: java.lang.Exception) {
                    // ACRA.getErrorReporter().maybeHandleSilentException(Throwable("Font loading issue: ${e.message}"), requireContext())
                    // Just to be sure
                    null
                }
            }

            refreshAlignment()

            fab = binding.fab

            fab.backgroundTintList = ColorStateList.valueOf(appColors.colorAccent)

            fab.rippleColor = appColors.colorAccentDark

            val floatingToolbar: FloatingToolbar = binding.floatingToolbar
            floatingToolbar.attachFab(fab)

            floatingToolbar.background = ColorDrawable(appColors.colorAccent)

            val customTabsIntent = requireActivity().buildCustomTabsIntent()
            mCustomTabActivityHelper = CustomTabActivityHelper()
            mCustomTabActivityHelper!!.bindCustomTabsService(activity)


            floatingToolbar.setClickListener(
                object : FloatingToolbar.ItemClickListener {
                    override fun onItemClick(item: MenuItem) {
                        when (item.itemId) {
                            R.id.more_action -> getContentFromMercury(customTabsIntent)
                            R.id.share_action -> requireActivity().shareLink(url, contentTitle)
                            R.id.open_action -> requireActivity().openInBrowserAsNewTask(this@ArticleFragment.item)
                            R.id.unread_action -> if (context != null) {
                                if (this@ArticleFragment.item.unread) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repository.markAsRead(this@ArticleFragment.item.id.toString())
                                    }
                                    this@ArticleFragment.item.unread = false
                                    Toast.makeText(
                                        context,
                                        R.string.marked_as_read,
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        repository.unmarkAsRead(this@ArticleFragment.item.id.toString())
                                    }
                                    this@ArticleFragment.item.unread = true
                                    Toast.makeText(
                                        context,
                                        R.string.marked_as_unread,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                            else -> Unit
                        }
                    }

                    override fun onItemLongClick(item: MenuItem?) {
                    }
                }
            )

            if (staticBar) {
                fab.hide()
                floatingToolbar.show()
            }

            binding.source.text = contentSource
            if (typeface != null) {
                binding.source.typeface = typeface
            }

            if (contentText.isEmptyOrNullOrNullString()) {
                getContentFromMercury(customTabsIntent)
            } else {
                binding.titleView.text = contentTitle
                if (typeface != null) {
                    binding.titleView.typeface = typeface
                }

                htmlToWebview()

                if (!contentImage.isEmptyOrNullOrNullString() && context != null) {
                    binding.imageView.visibility = View.VISIBLE
                    Glide
                        .with(requireContext())
                        .asBitmap()
                        .loadMaybeBasicAuth(config, contentImage)
                        .apply(RequestOptions.fitCenterTransform())
                        .into(binding.imageView)
                } else {
                    binding.imageView.visibility = View.GONE
                }
            }

            binding.nestedScrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    if (scrollY > oldScrollY) {
                        floatingToolbar.hide()
                        fab.hide()
                    } else {
                        if (staticBar) {
                            floatingToolbar.show()
                        } else {
                            if (floatingToolbar.isShowing) floatingToolbar.hide() else fab.show()
                        }
                    }
                }
            )

        } catch (e: InflateException) {
            AlertDialog.Builder(requireContext())
                .setMessage(requireContext().getString(R.string.webview_dialog_issue_message))
                .setTitle(requireContext().getString(R.string.webview_dialog_issue_title))
                .setPositiveButton(android.R.string.ok
                ) { _, _ ->
                    settings.putBoolean("prefer_article_viewer", false)
                    requireActivity().finish()
                }
                .create()
                .show()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun refreshAlignment() {
        textAlignment = when (settings.getInt("text_align", 1)) {
            1 -> "justify"
            2 -> "left"
            else -> "justify"
        }
    }

    private fun getContentFromMercury(customTabsIntent: CustomTabsIntent) {
        if ((context != null && requireContext().isNetworkAvailable(null)) || context == null) {
            binding.progressBar.visibility = View.VISIBLE
            val parser = MercuryApi()

            parser.parseUrl(url).enqueue(
                object : Callback<ParsedContent> {
                    override fun onResponse(
                        call: Call<ParsedContent>,
                        response: Response<ParsedContent>
                    ) {
                        // TODO: clean all the following after finding the mercury content issue
                        try {
                            if (response.body() != null && response.body()!!.content != null && !response.body()!!.content.isNullOrEmpty()) {
                                try {
                                    binding.titleView.text = response.body()!!.title
                                    if (typeface != null) {
                                        binding.titleView.typeface = typeface
                                    }
                                    try {
                                        // Note: Mercury may return relative urls... If it does the url val will not be changed.
                                        URL(response.body()!!.url)
                                        url = response.body()!!.url
                                    } catch (e: MalformedURLException) {
                                        // Mercury returned a relative url. We do nothing.
                                    }
                                } catch (e: Exception) {
                                }

                                try {
                                    contentText = response.body()!!.content.orEmpty()
                                    htmlToWebview()
                                } catch (e: Exception) {
                                }

                                try {
                                    if (response.body()!!.lead_image_url != null && !response.body()!!.lead_image_url.isNullOrEmpty() && context != null) {
                                        binding.imageView.visibility = View.VISIBLE
                                        try {
                                            Glide
                                                .with(requireContext())
                                                .asBitmap()
                                                .loadMaybeBasicAuth(config, response.body()!!.lead_image_url.orEmpty())
                                                .apply(RequestOptions.fitCenterTransform())
                                                .into(binding.imageView)
                                        } catch (e: Exception) {
                                        }
                                    } else {
                                        binding.imageView.visibility = View.GONE
                                    }
                                } catch (e: Exception) {
                                    if (context != null) {
                                    }
                                }

                                try {
                                    binding.nestedScrollView.scrollTo(0, 0)

                                    binding.progressBar.visibility = View.GONE
                                } catch (e: Exception) {
                                    if (context != null) {
                                    }
                                }
                            } else {
                                try {
                                    openInBrowserAfterFailing(customTabsIntent)
                                } catch (e: Exception) {
                                    if (context != null) {
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (context != null) {
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<ParsedContent>,
                        t: Throwable
                    ) = openInBrowserAfterFailing(customTabsIntent)
                }
            )
        }
    }

    private fun htmlToWebview() {
        val stringColor = String.format("#%06X", 0xFFFFFF and appColors.colorAccent)

        val attrs: IntArray = intArrayOf(android.R.attr.fontFamily)
        val a: TypedArray = requireContext().obtainStyledAttributes(resId, attrs)


        binding.webcontent.settings.standardFontFamily = a.getString(0)
        binding.webcontent.visibility = View.VISIBLE

        // TODO: Set the color strings programmatically
        val (stringTextColor, stringBackgroundColor) = if (appColors.isDarkTheme) {
            Pair("#FFFFFF", "#303030")
        } else {
            Pair("#212121", "#FAFAFA")
        }

        binding.webcontent.settings.useWideViewPort = true
        binding.webcontent.settings.loadWithOverviewMode = true
        binding.webcontent.settings.javaScriptEnabled = false

        binding.webcontent.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url : String): Boolean {
                if (binding.webcontent.hitTestResult.type != WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    requireContext().startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                return true
            }

            override fun shouldInterceptRequest(view: WebView?, url: String): WebResourceResponse? {
                val glideOptions = RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL)
                if (url.lowercase(Locale.US).contains(".jpg") || url.lowercase(Locale.US).contains(".jpeg")) {
                    try {
                        val image = Glide.with(view).asBitmap().apply(glideOptions).load(url).submit().get()
                        return WebResourceResponse("image/jpg", "UTF-8", getBitmapInputStream(image, Bitmap.CompressFormat.JPEG))
                    }catch ( e : ExecutionException) {}
                }
                else if (url.lowercase(Locale.US).contains(".png")) {
                    try {
                        val image = Glide.with(view).asBitmap().apply(glideOptions).load(url).submit().get()
                        return WebResourceResponse("image/jpg", "UTF-8", getBitmapInputStream(image, Bitmap.CompressFormat.PNG))
                    }catch ( e : ExecutionException) {}
                }
                else if (url.lowercase(Locale.US).contains(".webp")) {
                    try {
                        val image = Glide.with(view).asBitmap().apply(glideOptions).load(url).submit().get()
                        return WebResourceResponse("image/jpg", "UTF-8", getBitmapInputStream(image, Bitmap.CompressFormat.WEBP))
                    }catch ( e : ExecutionException) {}
                }

                return super.shouldInterceptRequest(view, url)
            }
        }

        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return performClick()
            }
        })

        binding.webcontent.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event)}

        binding.webcontent.settings.layoutAlgorithm =
                WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

        var baseUrl: String? = null

        try {
            val itemUrl = URL(url)
            baseUrl = itemUrl.protocol + "://" + itemUrl.host
        } catch (e: MalformedURLException) {
        }

        val fontName =  when (font) {
            getString(R.string.open_sans_font_id) -> "Open Sans"
            getString(R.string.roboto_font_id) -> "Roboto"
            else -> ""
        }

        val fontLinkAndStyle = if (font.isNotEmpty()) {
            """<link href="https://fonts.googleapis.com/css?family=${fontName.replace(" ", "+")}" rel="stylesheet">
                |<style>
                |   * {
                |       font-family: '$fontName';
                |   }
                |</style>
            """.trimMargin()
        } else {
            ""
        }

        binding.webcontent.loadDataWithBaseURL(
            baseUrl,
            """<html>
                |<head>
                |   <meta name="viewport" content="width=device-width, initial-scale=1">
                |   <style>
                |      img {
                |        display: inline-block;
                |        height: auto;
                |        width: 100%;
                |        max-width: 100%;
                |      }
                |      a {
                |        color: $stringColor !important;
                |      }
                |      *:not(a) {
                |        color: $stringTextColor;
                |      }
                |      * {
                |        font-size: ${fontSize}px;
                |        text-align: $textAlignment;
                |        word-break: break-word;
                |        overflow:hidden;
                |        line-height: 1.5em;
                |        background-color: $stringBackgroundColor;
                |      }
                |      body, html {
                |        background-color: $stringBackgroundColor !important;
                |        border-color: $stringBackgroundColor  !important;
                |        padding: 0 !important;
                |        margin: 0 !important;
                |      }
                |      a, pre, code {
                |        text-align: $textAlignment;
                |      }
                |      pre, code {
                |        white-space: pre-wrap;
                |        width:100%;
                |        background-color: $stringBackgroundColor;
                |      }
                |   </style>
                |   $fontLinkAndStyle
                |</head>
                |<body>
                |   $contentText
                |</body>""".trimMargin(),
            "text/html",
            "utf-8",
            null
        )
    }

    fun scrollDown() {
        val height = binding.nestedScrollView.measuredHeight
        binding.nestedScrollView.smoothScrollBy(0, height/2)
    }

    fun scrollUp() {
        val height = binding.nestedScrollView.measuredHeight
        binding.nestedScrollView.smoothScrollBy(0, -height/2)
    }

    private fun openInBrowserAfterFailing(customTabsIntent: CustomTabsIntent) {
        binding.progressBar.visibility = View.GONE
        requireActivity().openItemUrlInternalBrowser(
                url,
                customTabsIntent,
                requireActivity()
        )
    }

    companion object {
        private const val ARG_ITEMS = "items"

        fun newInstance(
                item: SelfossModel.Item
        ): ArticleFragment {
            val fragment = ArticleFragment()
            val args = Bundle()
            args.putParcelable(ARG_ITEMS, item.toParcelable())
            fragment.arguments = args
            return fragment
        }
    }

    fun performClick(): Boolean {
        if (binding.webcontent.hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                binding.webcontent.hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            val position : Int = allImages.indexOf(binding.webcontent.hitTestResult.extra)

            val intent = Intent(activity, ImageActivity::class.java)
            intent.putExtra("allImages", allImages)
            intent.putExtra("position", position)
            startActivity(intent)
            return false
        }
        return false
    }


}
