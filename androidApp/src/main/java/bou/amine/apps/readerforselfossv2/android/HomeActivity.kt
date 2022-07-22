package bou.amine.apps.readerforselfossv2.android

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.doOnNextLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.*
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import bou.amine.apps.readerforselfossv2.android.adapters.ItemCardAdapter
import bou.amine.apps.readerforselfossv2.android.adapters.ItemListAdapter
import bou.amine.apps.readerforselfossv2.android.adapters.ItemsAdapter
import bou.amine.apps.readerforselfossv2.android.background.LoadingWorker
import bou.amine.apps.readerforselfossv2.android.databinding.ActivityHomeBinding
import bou.amine.apps.readerforselfossv2.android.model.getIcon
import bou.amine.apps.readerforselfossv2.android.model.getTitleDecoded
import bou.amine.apps.readerforselfossv2.android.persistence.AndroidDeviceDatabase
import bou.amine.apps.readerforselfossv2.android.persistence.AndroidDeviceDatabaseService
import bou.amine.apps.readerforselfossv2.android.persistence.database.AppDatabase
import bou.amine.apps.readerforselfossv2.android.persistence.entities.ActionEntity
import bou.amine.apps.readerforselfossv2.android.persistence.entities.AndroidItemEntity
import bou.amine.apps.readerforselfossv2.android.persistence.migrations.MIGRATION_1_2
import bou.amine.apps.readerforselfossv2.android.persistence.migrations.MIGRATION_2_3
import bou.amine.apps.readerforselfossv2.android.persistence.migrations.MIGRATION_3_4
import bou.amine.apps.readerforselfossv2.android.settings.SettingsActivity
import bou.amine.apps.readerforselfossv2.android.themes.AppColors
import bou.amine.apps.readerforselfossv2.android.themes.Toppings
import bou.amine.apps.readerforselfossv2.android.utils.Config
import bou.amine.apps.readerforselfossv2.android.utils.bottombar.maybeShow
import bou.amine.apps.readerforselfossv2.android.utils.bottombar.removeBadge
import bou.amine.apps.readerforselfossv2.android.utils.customtabs.CustomTabActivityHelper
import bou.amine.apps.readerforselfossv2.android.utils.network.isNetworkAvailable
import bou.amine.apps.readerforselfossv2.android.utils.persistence.toEntity
import bou.amine.apps.readerforselfossv2.android.utils.persistence.toView
import bou.amine.apps.readerforselfossv2.repository.Repository

import bou.amine.apps.readerforselfossv2.utils.DateUtils
import bou.amine.apps.readerforselfossv2.rest.SelfossApiImpl
import bou.amine.apps.readerforselfossv2.rest.SelfossModel
import bou.amine.apps.readerforselfossv2.service.ApiDetailsService
import bou.amine.apps.readerforselfossv2.service.SearchService
import bou.amine.apps.readerforselfossv2.service.SelfossService
import bou.amine.apps.readerforselfossv2.utils.longHash
import com.ashokvarma.bottomnavigation.BottomNavigationBar
import com.ashokvarma.bottomnavigation.BottomNavigationItem
import com.ashokvarma.bottomnavigation.TextBadgeItem
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ftinc.scoop.Scoop
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.ProfileDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.util.addStickyFooterItem
import com.mikepenz.materialdrawer.util.updateBadge
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import org.kodein.di.instance
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class HomeActivity : AppCompatActivity(), SearchView.OnQueryTextListener, DIAware {

    private lateinit var dataBase: AndroidDeviceDatabase
    private lateinit var dbService: AndroidDeviceDatabaseService
    private lateinit var searchService: SearchService
    private lateinit var service: SelfossService<AndroidItemEntity>
    private val MENU_PREFERENCES = 12302
    private val DRAWER_ID_TAGS = 100101L
    private val DRAWER_ID_HIDDEN_TAGS = 101100L
    private val DRAWER_ID_SOURCES = 100110L
    private val DRAWER_ID_FILTERS = 100111L
    private val UNREAD_SHOWN = 1
    private val READ_SHOWN = 2
    private val FAV_SHOWN = 3

    private var items: ArrayList<SelfossModel.Item> = ArrayList()
    private var allItems: ArrayList<SelfossModel.Item> = ArrayList()

    private var internalBrowser = false
    private var articleViewer = false
    private var shouldBeCardView = false
    private var displayUnreadCount = false
    private var displayAllCount = false
    private var fullHeightCards: Boolean = false
    private var itemsNumber: Int = 200
    private var elementsShown: Int = 1
    private var userIdentifier: String = ""
    private var displayAccountHeader: Boolean = false
    private var infiniteScroll: Boolean = false
    private var lastFetchDone: Boolean = false
    private var updateSources: Boolean = true
    private var markOnScroll: Boolean = false
    private var hiddenTags: List<String> = emptyList()
    private var apiVersionMajor: Int = 0

    private var periodicRefresh = false
    private var refreshMinutes: Long = 360L
    private var refreshWhenChargingOnly = false

    private lateinit var tabNewBadge: TextBadgeItem
    private lateinit var tabArchiveBadge: TextBadgeItem
    private lateinit var tabStarredBadge: TextBadgeItem
    private lateinit var api: SelfossApiImpl
    private lateinit var customTabActivityHelper: CustomTabActivityHelper
    private lateinit var appColors: AppColors
    private var offset: Int = 0
    private var firstVisible: Int = 0
    private lateinit var recyclerViewScrollListener: RecyclerView.OnScrollListener
    private var settings = Settings()
    private lateinit var binding: ActivityHomeBinding

    private var recyclerAdapter: RecyclerView.Adapter<*>? = null

    private var fromTabShortcut: Boolean = false
    private var offlineShortcut: Boolean = false

    private lateinit var tagsBadge: Map<Long, Int>

    private lateinit var db: AppDatabase

    private lateinit var config: Config

    override val di by closestDI()
    private val apiDetailsService : ApiDetailsService by instance()
    private val repository : Repository by instance()

    data class DrawerData(val tags: List<SelfossModel.Tag>?, val sources: List<SelfossModel.Source>?)

    override fun onStart() {
        super.onStart()
        customTabActivityHelper.bindCustomTabsService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(this@HomeActivity)
        config = Config()

        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        val view = binding.root

        fromTabShortcut =  intent.getIntExtra("shortcutTab", -1) != -1
        offlineShortcut =  intent.getBooleanExtra("startOffline", false)

        if (fromTabShortcut) {
            elementsShown = intent.getIntExtra("shortcutTab", UNREAD_SHOWN)
        }

        setContentView(view)

        handleThemeBinding()

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        val mDrawerToggle = ActionBarDrawerToggle(this, binding.drawerContainer, binding.toolBar, R.string.material_drawer_open, R.string.material_drawer_close)
        binding.drawerContainer.addDrawerListener(mDrawerToggle)
        mDrawerToggle.syncState()

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "selfoss-database"
        ).addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3).addMigrations(MIGRATION_3_4).build()


        customTabActivityHelper = CustomTabActivityHelper()

        api = SelfossApiImpl(
//            this,
//            this@HomeActivity,
//            settings.getBoolean("isSelfSignedCert", false),
//            sharedPref.getString("api_timeout", "-1")!!.toLong()
            apiDetailsService
        )

        dataBase = AndroidDeviceDatabase(applicationContext)
        searchService = SearchService(DateUtils(repository.apiMajorVersion))
        dbService = AndroidDeviceDatabaseService(dataBase, searchService)
        service = SelfossService(api, dbService, searchService)
        items = ArrayList()
        allItems = ArrayList()

        handleBottomBar()
        handleDrawer()

        handleSwipeRefreshLayout()

        handleSettings()

        getApiMajorVersion()

        getElementsAccordingToTab()
    }

    private fun handleSwipeRefreshLayout() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.refresh_progress_1,
            R.color.refresh_progress_2,
            R.color.refresh_progress_3
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            offlineShortcut = false
            allItems = ArrayList()
            lastFetchDone = false
            handleDrawerItems()
            CoroutineScope(Dispatchers.Main).launch {
                getElementsAccordingToTab()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int =
                    if (elementsShown == FAV_SHOWN) {
                        0
                    } else {
                        super.getSwipeDirs(
                            recyclerView,
                            viewHolder
                        )
                    }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    val i = items.elementAtOrNull(position)

                    if (i != null) {
                        val adapter = binding.recyclerView.adapter as ItemsAdapter<*>

                        adapter.handleItemAtIndex(position)

                        reloadBadgeContent()

                        val tagHashes = i.tags.map { it.longHash() }
                        tagsBadge = tagsBadge.map {
                            if (tagHashes.contains(it.key)) {
                                (it.key to (it.value - 1))
                            } else {
                                (it.key to it.value)
                            }
                        }.toMap()
                        reloadTagsBadges()

                        // Just load everythin
                        if (items.size <= 0) {
                            getElementsAccordingToTab()
                        }
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            "Found null when swiping at positon $position.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun handleBottomBar() {

        tabNewBadge = TextBadgeItem()
            .setText("")
            .setHideOnSelect(false).hide(false)
            .setBackgroundColor(appColors.colorPrimary)
        tabArchiveBadge = TextBadgeItem()
            .setText("")
            .setHideOnSelect(false).hide(false)
            .setBackgroundColor(appColors.colorPrimary)
        tabStarredBadge = TextBadgeItem()
            .setText("")
            .setHideOnSelect(false).hide(false)
            .setBackgroundColor(appColors.colorPrimary)

        val tabNew =
            BottomNavigationItem(
                R.drawable.ic_tab_fiber_new_black_24dp,
                getString(R.string.tab_new)
            ).setActiveColor(appColors.colorAccent)
                .setBadgeItem(tabNewBadge)
        val tabArchive =
            BottomNavigationItem(
                R.drawable.ic_tab_archive_black_24dp,
                getString(R.string.tab_read)
            ).setActiveColor(appColors.colorAccentDark)
                .setBadgeItem(tabArchiveBadge)
        val tabStarred =
            BottomNavigationItem(
                R.drawable.ic_tab_favorite_black_24dp,
                getString(R.string.tab_favs)
            ).setActiveColorResource(R.color.pink)
                .setBadgeItem(tabStarredBadge)

        binding.bottomBar
            .addItem(tabNew)
            .addItem(tabArchive)
            .addItem(tabStarred)
            .setFirstSelectedPosition(0)
            .initialise()
        binding.bottomBar.setMode(BottomNavigationBar.MODE_SHIFTING)
        binding.bottomBar.setBackgroundStyle(BottomNavigationBar.BACKGROUND_STYLE_STATIC)

        if (fromTabShortcut) {
            binding.bottomBar.selectTab(elementsShown - 1)
        }
    }

    private fun getApiMajorVersion() {
        CoroutineScope(Dispatchers.IO).launch {
            val version = api.version()
            if (version != null) {
                apiVersionMajor = version.getApiMajorVersion()
                settings.putInt("apiVersionMajor", apiVersionMajor)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // TODO: Make this the only appcolors init
        appColors = AppColors(this@HomeActivity)

        handleDrawerItems()

        handleThemeUpdate()

        reloadLayoutManager()

        if (!infiniteScroll) {
            binding.recyclerView.setHasFixedSize(true)
        } else {
            handleInfiniteScroll()
        }

        handleBottomBarActions()

        handleRecurringTask()

        handleOfflineActions()

        getElementsAccordingToTab()
    }

    override fun onStop() {
        super.onStop()
        customTabActivityHelper.unbindCustomTabsService(this)
    }

    private fun handleSettings() {
        internalBrowser = settings.getBoolean("prefer_internal_browser", true)
        articleViewer = settings.getBoolean("prefer_article_viewer", true)
        shouldBeCardView = settings.getBoolean("card_view_active", false)
        displayUnreadCount = settings.getBoolean("display_unread_count", true)
        displayAllCount = settings.getBoolean("display_other_count", false)
        fullHeightCards = settings.getBoolean("full_height_cards", false)
        itemsNumber = settings.getString("prefer_api_items_number", "200").toInt()
        userIdentifier = settings.getString("unique_id", "")
        displayAccountHeader = settings.getBoolean("account_header_displaying", false)
        infiniteScroll = settings.getBoolean("infinite_loading", false)
        searchService.itemsCaching = settings.getBoolean("items_caching", false)
        updateSources = settings.getBoolean("update_sources", true)
        markOnScroll = settings.getBoolean("mark_on_scroll", false)
        hiddenTags = if (settings.getString("hidden_tags", "").isNotEmpty()) {
            settings.getString("hidden_tags", "").replace("\\s".toRegex(), "").split(",")
        } else {
            emptyList()
        }
        periodicRefresh = settings.getBoolean("periodic_refresh", false)
        refreshWhenChargingOnly = settings.getBoolean("refresh_when_charging", false)
        refreshMinutes = settings.getString("periodic_refresh_minutes", "360").toLong()

        if (refreshMinutes <= 15) {
            refreshMinutes = 15
        }

        apiVersionMajor = settings.getInt("apiVersionMajor", 0)
    }

    private fun handleThemeBinding() {
        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, binding.toolBar)
        scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)
    }

    private fun handleThemeUpdate() {

        val scoop = Scoop.getInstance()
        scoop.update(Toppings.PRIMARY.value, appColors.colorPrimary)

        scoop.update(Toppings.PRIMARY_DARK.value, appColors.colorPrimaryDark)
    }

    private fun handleDrawer() {
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView, uri: Uri, placeholder: Drawable, tag: String?) {
                Glide.with(this@HomeActivity)
                        .asBitmap()
                        .load(uri)
                        .apply(RequestOptions()
                                .placeholder(R.mipmap.ic_launcher)
                                .fallback(R.mipmap.ic_launcher)
                                .fitCenter())
                        .into(imageView)
            }

            override fun cancel(imageView: ImageView) {
                Glide.with(this@HomeActivity).clear(imageView)
            }
        })

        val drawerListener = object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
                binding.bottomBar.hide()
            }

            override fun onDrawerClosed(drawerView: View) {
                binding.bottomBar.show()
            }

            override fun onDrawerStateChanged(newState: Int) {
            }

        }

        binding.drawerContainer.addDrawerListener(drawerListener)

        displayAccountHeader =
                settings.getBoolean("account_header_displaying", false)

        binding.mainDrawer.addStickyFooterItem(
            PrimaryDrawerItem().apply {
                nameRes = R.string.drawer_report_bug
                iconRes = R.drawable.ic_bug_report_black_24dp
                isIconTinted = true
                onDrawerItemClickListener = { _, _, _ ->
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Config.trackerUrl))
                    startActivity(browserIntent)
                    false
                }
            })

        binding.mainDrawer.addStickyFooterItem(
            PrimaryDrawerItem().apply {
                nameRes = R.string.title_activity_settings
                iconRes = R.drawable.ic_settings_black_24dp
                isIconTinted = true
                onDrawerItemClickListener = { _, _, _ ->
                    startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
                    false
                }
            })

        if (displayAccountHeader) {
            AccountHeaderView(this).apply {
                attachToSliderView(binding.mainDrawer)
                addProfiles(
                    ProfileDrawerItem().apply {
                        nameText = settings.getString("url", "")
                        setBackgroundResource(R.drawable.bg)
                        iconRes = R.mipmap.ic_launcher
                        selectionListEnabledForSingleProfile = false
                    }
                )
            }
        }
    }

    private fun handleDrawerItems() {
        tagsBadge = emptyMap()
        fun handleDrawerData(maybeDrawerData: DrawerData?, loadedFromCache: Boolean = false) {
            fun handleTags(maybeTags: List<SelfossModel.Tag>?) {
                if (maybeTags == null) {
                    if (loadedFromCache) {
                        binding.mainDrawer.itemAdapter.add(
                            SecondaryDrawerItem()
                                .apply { nameRes = R.string.drawer_error_loading_tags; isSelectable = false }
                        )
                    }
                } else {
                    val filteredTags = maybeTags
                        .filterNot { hiddenTags.contains(it.tag) }
                        .sortedBy { it.unread == 0 }
                    tagsBadge = filteredTags.map {
                        val gd = GradientDrawable()
                        val gdColor = try {
                            Color.parseColor(it.color)
                        } catch (e: IllegalArgumentException) {
                            appColors.colorPrimary
                        }

                        gd.setColor(gdColor)
                        gd.shape = GradientDrawable.RECTANGLE
                        gd.setSize(30, 30)
                        gd.cornerRadius = 30F
                        val drawerItem =
                            PrimaryDrawerItem()
                                .apply {
                                    nameText = it.getTitleDecoded()
                                    identifier = it.tag.longHash()
                                    iconDrawable = gd
                                    badgeStyle = BadgeStyle().apply {
                                        textColor = ColorHolder.fromColor(Color.WHITE)
                                        color = ColorHolder.fromColor(appColors.colorAccent) }
                                    onDrawerItemClickListener = { _,_,_ ->
                                        allItems = ArrayList()
                                        searchService.tagFilter = it.tag
                                        searchService.sourceFilter = null
                                        searchService.sourceIDFilter = null
                                        getElementsAccordingToTab()
                                        fetchOnEmptyList()
                                        false
                                    } }
                        if (it.unread > 0) {
                            drawerItem.badgeText = it.unread.toString()
                        }

                        binding.mainDrawer.itemAdapter.add(drawerItem)

                        (it.tag.longHash() to it.unread)
                    }.toMap()
                }
            }

            fun handleHiddenTags(maybeTags: List<SelfossModel.Tag>?) {
                if (maybeTags == null) {
                    if (loadedFromCache) {
                        binding.mainDrawer.itemAdapter.add(
                            SecondaryDrawerItem().apply {
                                nameRes = R.string.drawer_error_loading_tags
                                isSelectable = false
                            }
                        )
                    }
                } else {
                    val filteredHiddenTags: List<SelfossModel.Tag> =
                        maybeTags.filter { hiddenTags.contains(it.tag) }
                    tagsBadge = filteredHiddenTags.map {
                        val gd = GradientDrawable()
                        val gdColor = try {
                            Color.parseColor(it.color)
                        } catch (e: IllegalArgumentException) {
                            appColors.colorPrimary
                        }

                        gd.setColor(gdColor)
                        gd.shape = GradientDrawable.RECTANGLE
                        gd.setSize(30, 30)
                        gd.cornerRadius = 30F
                        val drawerItem =
                            PrimaryDrawerItem().apply {
                                nameText = it.getTitleDecoded()
                                identifier = it.tag.longHash()
                                iconDrawable = gd
                                badgeStyle = BadgeStyle().apply {
                                    textColor = ColorHolder.fromColor(Color.WHITE)
                                    color = ColorHolder.fromColor(appColors.colorAccent) }
                                onDrawerItemClickListener = { _,_,_ ->
                                    allItems = ArrayList()
                                    searchService.tagFilter = it.tag
                                    searchService.sourceFilter = null
                                    searchService.sourceIDFilter = null
                                    getElementsAccordingToTab()
                                    fetchOnEmptyList()
                                    false
                                } }

                        if (it.unread > 0) {
                            drawerItem.badgeText = it.unread.toString()
                        }
                        binding.mainDrawer.itemAdapter.add(drawerItem)

                        (it.tag.longHash() to it.unread)
                    }.toMap()
                }
            }

            fun handleSources(maybeSources: List<SelfossModel.Source>?) {
                if (maybeSources == null) {
                    if (loadedFromCache) {
                        binding.mainDrawer.itemAdapter.add(
                            SecondaryDrawerItem().apply {
                                nameRes = R.string.drawer_error_loading_sources
                                isSelectable = false
                            }
                        )
                    }
                } else {
                    for (source in maybeSources) {
                        val item = PrimaryDrawerItem().apply {
                            nameText = source.getTitleDecoded()
                            identifier = source.id.toLong()
                            iconUrl = source.getIcon(apiDetailsService.getBaseUrl())
                            onDrawerItemClickListener = { _,_,_ ->
                                allItems = ArrayList()
                                searchService.sourceIDFilter = source.id.toLong()
                                searchService.sourceFilter = source.title
                                searchService.tagFilter = null
                                getElementsAccordingToTab()
                                fetchOnEmptyList()
                                false
                            }
                        }
                        binding.mainDrawer.itemAdapter.add(item)
                    }
                }
            }

            binding.mainDrawer.itemAdapter.clear()
            if (maybeDrawerData != null) {
                binding.mainDrawer.itemAdapter.add(
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.drawer_item_filters
                        isSelectable = false
                        identifier = DRAWER_ID_FILTERS
                        badgeRes = R.string.drawer_action_clear
                        onDrawerItemClickListener = { _,_,_ ->
                            allItems = ArrayList()
                            searchService.sourceFilter = null
                            searchService.sourceIDFilter = null
                            searchService.tagFilter = null
                            binding.mainDrawer.setSelectionAtPosition(-1)
                            getElementsAccordingToTab()
                            fetchOnEmptyList()
                            false
                        }
                    }
                )
                if (hiddenTags.isNotEmpty()) {
                    binding.mainDrawer.itemAdapter.add(
                        DividerDrawerItem(),
                        SecondaryDrawerItem().apply {
                            nameRes = R.string.drawer_item_hidden_tags
                            identifier = DRAWER_ID_HIDDEN_TAGS
                            isSelectable = false
                        }
                    )
                    handleHiddenTags(maybeDrawerData.tags)
                }
                binding.mainDrawer.itemAdapter.add(
                    DividerDrawerItem(),
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.drawer_item_tags
                        identifier = DRAWER_ID_TAGS
                        isSelectable = false
                    }
                )
                handleTags(maybeDrawerData.tags)
                binding.mainDrawer.itemAdapter.add(
                    DividerDrawerItem(),
                    SecondaryDrawerItem().apply {
                        nameRes = R.string.drawer_item_sources
                        identifier = DRAWER_ID_SOURCES
                        isSelectable = false
                        badgeRes = R.string.drawer_action_edit
                        onDrawerItemClickListener = { v,_,_ ->
                            startActivity(Intent(v!!.context, SourcesActivity::class.java))
                            false
                        }
                    }
                )
                handleSources(maybeDrawerData.sources)
                binding.mainDrawer.itemAdapter.add(
                    DividerDrawerItem(),
                    PrimaryDrawerItem().apply {
                        nameRes = R.string.action_about
                        isSelectable = false
                        iconRes = R.drawable.ic_info_outline_white_24dp
                        isIconTinted = true
                        onDrawerItemClickListener = { _,_,_ ->
                            LibsBuilder()
                                .withAboutIconShown(true)
                                .withAboutVersionShown(true)
                                .start(this@HomeActivity)
                            false
                        }
                    }
                )

                if (!loadedFromCache) {
                    if (maybeDrawerData.tags != null) {
                        thread {
                            val tagEntities = maybeDrawerData.tags.map { it.toEntity() }
                            db.drawerDataDao().deleteAllTags()
                            db.drawerDataDao().insertAllTags(*tagEntities.toTypedArray())
                        }
                    }
                    if (maybeDrawerData.sources != null) {
                        thread {
                            val sourceEntities =
                                maybeDrawerData.sources.map { it.toEntity() }
                            db.drawerDataDao().deleteAllSources()
                            db.drawerDataDao().insertAllSources(*sourceEntities.toTypedArray())
                        }
                    }
                }
            } else {
                if (!loadedFromCache) {
                    binding.mainDrawer.itemAdapter.add(
                        PrimaryDrawerItem().apply {
                            nameRes = R.string.no_tags_loaded
                            identifier = DRAWER_ID_TAGS
                            isSelectable = false
                        },
                        PrimaryDrawerItem().apply {
                            nameRes = R.string.no_sources_loaded
                            identifier = DRAWER_ID_SOURCES
                            isSelectable = false
                        }
                    )
                }
            }
        }

        fun drawerApiCalls(maybeDrawerData: DrawerData?) {
            var tags: List<SelfossModel.Tag>? = null
            var sources: List<SelfossModel.Source>?

            fun sourcesApiCall() {
                if (this@HomeActivity.isNetworkAvailable(null, offlineShortcut) && updateSources) {
                    CoroutineScope(Dispatchers.Main).launch {
                        val response = api.sources()
                        if (response != null) {
                            sources = response
                            val apiDrawerData = DrawerData(tags, sources)
                            if ((maybeDrawerData != null && maybeDrawerData != apiDrawerData) || maybeDrawerData == null) {
                                handleDrawerData(apiDrawerData)
                            }
                        } else {
                            val apiDrawerData = DrawerData(tags, null)
                            if ((maybeDrawerData != null && maybeDrawerData != apiDrawerData) || maybeDrawerData == null) {
                                handleDrawerData(apiDrawerData)
                            }
                        }
                    }
                }
            }

            if (this@HomeActivity.isNetworkAvailable(null, offlineShortcut) && updateSources) {
                CoroutineScope(Dispatchers.IO).launch {
                    val response = api.tags()
                    if (response != null) {
                        tags = response
                    }
                    sourcesApiCall()
                }
            }
        }

        binding.mainDrawer.itemAdapter.add(
            PrimaryDrawerItem().apply {
                nameRes = R.string.drawer_loading
                isSelectable = false
            }
        )

        thread {
            val drawerData = DrawerData(db.drawerDataDao().tags().map { it.toView() },
                                        db.drawerDataDao().sources().map { it.toView() })
            runOnUiThread {
                handleDrawerData(drawerData, loadedFromCache = true)
                drawerApiCalls(drawerData)
            }
        }
    }

    private fun reloadLayoutManager() {
        val currentManager = binding.recyclerView.layoutManager
        val layoutManager: RecyclerView.LayoutManager

        // This will only update the layout manager if settings changed
        when (currentManager) {
            is StaggeredGridLayoutManager ->
                if (!shouldBeCardView) {
                    layoutManager = GridLayoutManager(
                        this,
                        calculateNoOfColumns()
                    )
                    binding.recyclerView.layoutManager = layoutManager
                }
            is GridLayoutManager ->
                if (shouldBeCardView) {
                    layoutManager = StaggeredGridLayoutManager(
                        calculateNoOfColumns(),
                        StaggeredGridLayoutManager.VERTICAL
                    )
                    layoutManager.gapStrategy =
                            StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
                    binding.recyclerView.layoutManager = layoutManager
                }
            else ->
                if (currentManager == null) {
                    if (!shouldBeCardView) {
                        layoutManager = GridLayoutManager(
                            this,
                            calculateNoOfColumns()
                        )
                        binding.recyclerView.layoutManager = layoutManager
                    } else {
                        layoutManager = StaggeredGridLayoutManager(
                            calculateNoOfColumns(),
                            StaggeredGridLayoutManager.VERTICAL
                        )
                        layoutManager.gapStrategy =
                                StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
                        binding.recyclerView.layoutManager = layoutManager
                    }
                }
        }
    }

    private fun handleBottomBarActions() {
        binding.bottomBar.setTabSelectedListener(object : BottomNavigationBar.OnTabSelectedListener {
            override fun onTabUnselected(position: Int) = Unit

            override fun onTabReselected(position: Int) {

                when (val layoutManager = binding.recyclerView.adapter) {
                    is StaggeredGridLayoutManager ->
                        if (layoutManager.findFirstCompletelyVisibleItemPositions(null)[0] == 0) {
                            getElementsAccordingToTab()
                        } else {
                            layoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    is GridLayoutManager ->
                        if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                            getElementsAccordingToTab()
                        } else {
                            layoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    else -> Unit
                }
            }

            override fun onTabSelected(position: Int) {
                offset = 0
                lastFetchDone = false

                elementsShown = position + 1
                getElementsAccordingToTab()
                binding.recyclerView.scrollToPosition(0)

                fetchOnEmptyList()
            }
        })
    }

    private fun fetchOnEmptyList() {
        binding.recyclerView.doOnNextLayout {
            // TODO: do if last element (or is empty ?)
            getElementsAccordingToTab(true)
        }
    }

    private fun handleInfiniteScroll() {
        recyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(localRecycler: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val lastVisibleItem = getLastVisibleItem()

                    if (lastVisibleItem == (items.size - 1) && items.size < maxItemNumber()) {
                        getElementsAccordingToTab(appendResults = true)
                    }
                }
            }
        }

        binding.recyclerView.clearOnScrollListeners()
        binding.recyclerView.addOnScrollListener(recyclerViewScrollListener)
    }

    private fun getLastVisibleItem() : Int {
        return when (val manager = binding.recyclerView.layoutManager) {
            is StaggeredGridLayoutManager -> manager.findLastCompletelyVisibleItemPositions(
                null
            ).last()
            is GridLayoutManager -> manager.findLastCompletelyVisibleItemPosition()
            else -> 0
        }
    }

    private fun mayBeEmpty() =
        if (items.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
        } else {
            binding.emptyText.visibility = View.GONE
        }

    private fun getElementsAccordingToTab(
        appendResults: Boolean = false
    ) {
        fun doGetAccordingToTab() {
            when (elementsShown) {
                UNREAD_SHOWN -> getUnRead(appendResults)
                READ_SHOWN -> getRead(appendResults)
                FAV_SHOWN -> getStarred(appendResults)
                else -> getUnRead(appendResults)
            }
        }

        offset = if (appendResults && items.size > 0) {
            items.size - 1
        } else {
            0
        }
        firstVisible = if (appendResults) firstVisible else 0

        doGetAccordingToTab()
    }

    private fun getUnRead(appendResults: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.swipeRefreshLayout.isRefreshing = true
            val apiItems = service.getUnreadItems(itemsNumber, offset, applicationContext.isNetworkAvailable())
            if (appendResults) {
                apiItems?.let { items.addAll(it) }
            } else {
                items = apiItems.orEmpty() as ArrayList<SelfossModel.Item>
            }
            binding.swipeRefreshLayout.isRefreshing = false
            handleListResult()
        }
    }

    private fun getRead(appendResults: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.swipeRefreshLayout.isRefreshing = true
            val apiItems = service.getReadItems(itemsNumber, offset, applicationContext.isNetworkAvailable())
            if (appendResults) {
                apiItems?.let { items.addAll(it) }
            } else {
                items = apiItems.orEmpty() as ArrayList<SelfossModel.Item>
            }
            binding.swipeRefreshLayout.isRefreshing = false
            handleListResult()
        }
    }

    private fun getStarred(appendResults: Boolean = false) {
        CoroutineScope(Dispatchers.Main).launch {
            binding.swipeRefreshLayout.isRefreshing = true
            val apiItems = service.getStarredItems(itemsNumber, offset, applicationContext.isNetworkAvailable())
            if (appendResults) {
                apiItems?.let { items.addAll(it) }
            } else {
                items = apiItems.orEmpty() as ArrayList<SelfossModel.Item>
            }
            binding.swipeRefreshLayout.isRefreshing = false
            handleListResult()
        }
    }

    private fun handleListResult(appendResults: Boolean = false) {
        if (appendResults) {
            val oldManager = binding.recyclerView.layoutManager
            firstVisible = when (oldManager) {
                is StaggeredGridLayoutManager ->
                    oldManager.findFirstCompletelyVisibleItemPositions(null).last()
                is GridLayoutManager ->
                    oldManager.findFirstCompletelyVisibleItemPosition()
                else -> 0
            }
        }

        if (recyclerAdapter == null) {
            if (shouldBeCardView) {
                recyclerAdapter =
                        ItemCardAdapter(
                            this,
                            items,
                            apiDetailsService,
                            db,
                            customTabActivityHelper,
                            internalBrowser,
                            articleViewer,
                            fullHeightCards,
                            appColors,
                            userIdentifier,
                            config,
                            searchService
                        ) {
                            updateItems(it)
                        }
            } else {
                recyclerAdapter =
                        ItemListAdapter(
                            this,
                            items,
                            apiDetailsService,
                            db,
                            customTabActivityHelper,
                            internalBrowser,
                            articleViewer,
                            userIdentifier,
                            appColors,
                            config,
                            searchService
                        ) {
                            updateItems(it)
                        }

                binding.recyclerView.addItemDecoration(
                    DividerItemDecoration(
                        this@HomeActivity,
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
            binding.recyclerView.adapter = recyclerAdapter
        } else {
                (recyclerAdapter as ItemsAdapter<*>).updateAllItems(items)
        }

        reloadBadges()
        mayBeEmpty()
    }

    private fun reloadBadges() {
        if (displayUnreadCount || displayAllCount) {
            CoroutineScope(Dispatchers.Main).launch {
                service.reloadBadges(applicationContext.isNetworkAvailable())
                reloadBadgeContent()
            }
        }
    }

    private fun reloadBadgeContent() {
        if (displayUnreadCount) {
            tabNewBadge
                .setText(searchService.badgeUnread.toString())
                .maybeShow()
        }
        if (displayAllCount) {
            tabArchiveBadge
                .setText(searchService.badgeAll.toString())
                .maybeShow()
            tabStarredBadge
                .setText(searchService.badgeStarred.toString())
                .maybeShow()
        }
    }

    private fun reloadTagsBadges() {
        tagsBadge.forEach {
            binding.mainDrawer.updateBadge(it.key, StringHolder(it.value.toString()))
        }
        binding.mainDrawer.resetDrawerContent()
    }

    private fun calculateNoOfColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / 300).toInt()
    }

    override fun onQueryTextChange(p0: String?): Boolean {
        if (p0.isNullOrBlank()) {
            searchService.searchFilter = null
            getElementsAccordingToTab()
            fetchOnEmptyList()
        }
        return false
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        searchService.searchFilter = p0
        getElementsAccordingToTab()
        fetchOnEmptyList()
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.home_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.getActionView() as SearchView
        searchView.setOnQueryTextListener(this)

        return true
    }

    private fun needsConfirmation(titleRes: Int, messageRes: Int, doFn: () -> Unit) {
        AlertDialog.Builder(this@HomeActivity)
            .setMessage(messageRes)
            .setTitle(titleRes)
            .setPositiveButton(android.R.string.ok) { _, _ -> doFn() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                if (this@HomeActivity.isNetworkAvailable(null, offlineShortcut)) {
                    needsConfirmation(R.string.menu_home_refresh, R.string.refresh_dialog_message) {
                        Toast.makeText(this, R.string.refresh_in_progress, Toast.LENGTH_SHORT).show()
                        CoroutineScope(Dispatchers.Main).launch {
                            val status = api.update()
                            if (status != null && status.isSuccess) {
                                Toast.makeText(
                                    this@HomeActivity,
                                    R.string.refresh_success_response, Toast.LENGTH_LONG
                                )
                                    .show()
                            } else {
                                Toast.makeText(
                                    this@HomeActivity,
                                    R.string.refresh_failer_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    return true
                } else {
                    return false
                }
            }
            R.id.readAll -> {
                if (elementsShown == UNREAD_SHOWN) {
                    needsConfirmation(R.string.readAll, R.string.markall_dialog_message) {
                        binding.swipeRefreshLayout.isRefreshing = true

                        if (this@HomeActivity.isNetworkAvailable(null, offlineShortcut)) {
                            CoroutineScope(Dispatchers.Main).launch {
                                val success = service.readAll(items.map { it.id.toString() }, applicationContext.isNetworkAvailable())
                                if (success) {
                                    Toast.makeText(
                                        this@HomeActivity,
                                        R.string.all_posts_read,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    tabNewBadge.removeBadge()

                                    handleDrawerItems()

                                    getElementsAccordingToTab()
                                } else {
                                    Toast.makeText(
                                        this@HomeActivity,
                                        R.string.all_posts_not_read,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                handleListResult()
                                binding.swipeRefreshLayout.isRefreshing = false
                            }
                        }
                    }
                }
                return true
            }
            R.id.action_disconnect -> {
                return Config.logoutAndRedirect(this, this@HomeActivity)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun maxItemNumber(): Int =
        when (elementsShown) {
            UNREAD_SHOWN -> searchService.badgeUnread
            READ_SHOWN -> searchService.badgeAll
            FAV_SHOWN -> searchService.badgeStarred
            else -> searchService.badgeUnread // if !elementsShown then unread are fetched.
        }

    private fun updateItems(adapterItems: ArrayList<SelfossModel.Item>) {
        items = adapterItems
    }

    private fun handleRecurringTask() {
        if (periodicRefresh) {
            val myConstraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(refreshWhenChargingOnly)
                .setRequiresStorageNotLow(true)
                .build()

            val backgroundWork =
                PeriodicWorkRequestBuilder<LoadingWorker>(refreshMinutes, TimeUnit.MINUTES)
                    .setConstraints(myConstraints)
                    .addTag("selfoss-loading")
                    .build()

            WorkManager.getInstance(baseContext).enqueueUniquePeriodicWork("selfoss-loading", ExistingPeriodicWorkPolicy.KEEP, backgroundWork)
        }
    }

    private fun handleOfflineActions() {
        fun doAndReportOnFail(call: SelfossModel.SuccessResponse?, action: ActionEntity) {
            if (call != null && call.isSuccess) {
                thread {
                    db.actionsDao().delete(action)
                }
            }
        }

        if (this@HomeActivity.isNetworkAvailable(null, offlineShortcut)) {
            CoroutineScope(Dispatchers.Main).launch {
                val actions = db.actionsDao().actions()

                actions.forEach { action ->
                    when {
                        action.read -> doAndReportOnFail(api.markAsRead(action.articleId), action)
                        action.unread -> doAndReportOnFail(api.unmarkAsRead(action.articleId), action)
                        action.starred -> doAndReportOnFail(api.starr(action.articleId), action)
                        action.unstarred -> doAndReportOnFail(api.unstarr(action.articleId), action)
                    }
                }
            }
        }
    }
}

