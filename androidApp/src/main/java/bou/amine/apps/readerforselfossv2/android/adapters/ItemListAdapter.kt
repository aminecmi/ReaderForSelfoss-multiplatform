package bou.amine.apps.readerforselfossv2.android.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import bou.amine.apps.readerforselfossv2.android.databinding.ListItemBinding
import bou.amine.apps.readerforselfossv2.android.model.*
import bou.amine.apps.readerforselfossv2.android.persistence.database.AppDatabase
import bou.amine.apps.readerforselfossv2.android.themes.AppColors
import bou.amine.apps.readerforselfossv2.android.utils.*
import bou.amine.apps.readerforselfossv2.android.utils.customtabs.CustomTabActivityHelper
import bou.amine.apps.readerforselfossv2.android.utils.glide.bitmapCenterCrop
import bou.amine.apps.readerforselfossv2.android.utils.glide.circularBitmapDrawable
import bou.amine.apps.readerforselfossv2.repository.Repository
import bou.amine.apps.readerforselfossv2.rest.SelfossModel
import bou.amine.apps.readerforselfossv2.service.ApiDetailsService
import bou.amine.apps.readerforselfossv2.service.SearchService
import bou.amine.apps.readerforselfossv2.utils.DateUtils
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import org.kodein.di.DI
import org.kodein.di.android.closestDI
import org.kodein.di.instance

class ItemListAdapter(
    override val app: Activity,
    override var items: ArrayList<SelfossModel.Item>,
    override val apiDetailsService: ApiDetailsService,
    override val db: AppDatabase,
    private val helper: CustomTabActivityHelper,
    private val internalBrowser: Boolean,
    private val articleViewer: Boolean,
    override val userIdentifier: String,
    override val appColors: AppColors,
    override val config: Config,
    override val searchService: SearchService,
    override val updateItems: (ArrayList<SelfossModel.Item>) -> Unit
) : ItemsAdapter<ItemListAdapter.ViewHolder>() {
    private val generator: ColorGenerator = ColorGenerator.MATERIAL
    private val c: Context = app.baseContext

    override val di: DI by closestDI(app)
    override val repository : Repository by instance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            val itm = items[position]

            binding.title.text = itm.getTitleDecoded()

            binding.title.setOnTouchListener(LinkOnTouchListener())

            binding.title.setLinkTextColor(appColors.colorAccent)

            binding.sourceTitleAndDate.text = itm.sourceAndDateText(DateUtils(repository.apiMajorVersion))

            if (itm.getThumbnail(apiDetailsService.getBaseUrl()).isEmpty()) {

                if (itm.getIcon(apiDetailsService.getBaseUrl()).isEmpty()) {
                    val color = generator.getColor(itm.getSourceTitle())

                    val drawable =
                            TextDrawable
                                    .builder()
                                    .round()
                                    .build(itm.getSourceTitle().toTextDrawableString(c), color)

                    binding.itemImage.setImageDrawable(drawable)
                } else {
                    c.circularBitmapDrawable(config, itm.getIcon(apiDetailsService.getBaseUrl()), binding.itemImage)
                }
            } else {
                c.bitmapCenterCrop(config, itm.getThumbnail(apiDetailsService.getBaseUrl()), binding.itemImage)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
            handleCustomTabActions()
        }

        private fun handleCustomTabActions() {
            val customTabsIntent = c.buildCustomTabsIntent()
            helper.bindCustomTabsService(app)

            binding.root.setOnClickListener {
                c.openItemUrl(
                    items,
                    bindingAdapterPosition,
                    items[bindingAdapterPosition].getLinkDecoded(),
                    customTabsIntent,
                    internalBrowser,
                    articleViewer,
                    app,
                    searchService
                )
            }
        }
    }
}
