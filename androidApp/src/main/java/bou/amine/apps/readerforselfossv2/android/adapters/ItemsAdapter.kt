package bou.amine.apps.readerforselfossv2.android.adapters

import android.app.Activity
import android.graphics.Color
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import bou.amine.apps.readerforselfossv2.android.R
import bou.amine.apps.readerforselfossv2.android.persistence.database.AppDatabase
import bou.amine.apps.readerforselfossv2.android.themes.AppColors
import bou.amine.apps.readerforselfossv2.android.utils.Config
import bou.amine.apps.readerforselfossv2.repository.Repository
import bou.amine.apps.readerforselfossv2.rest.SelfossModel
import bou.amine.apps.readerforselfossv2.service.ApiDetailsService
import bou.amine.apps.readerforselfossv2.service.SearchService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.kodein.di.DIAware

abstract class ItemsAdapter<VH : RecyclerView.ViewHolder?> : RecyclerView.Adapter<VH>(), DIAware {
    abstract var items: ArrayList<SelfossModel.Item>
    abstract val apiDetailsService: ApiDetailsService
    abstract val repository: Repository
    abstract val db: AppDatabase
    abstract val userIdentifier: String
    abstract val app: Activity
    abstract val appColors: AppColors
    abstract val config: Config
    abstract val searchService: SearchService
    abstract val updateItems: (ArrayList<SelfossModel.Item>) -> Unit

    fun updateAllItems(items: ArrayList<SelfossModel.Item>) {
        this.items = items
        notifyDataSetChanged()
        updateItems(this.items)
    }

    private fun unmarkSnackbar(i: SelfossModel.Item, position: Int) {
        val s = Snackbar
            .make(
                app.findViewById(R.id.coordLayout),
                R.string.marked_as_read,
                Snackbar.LENGTH_LONG
            )
            .setAction(R.string.undo_string) {
                CoroutineScope(Dispatchers.IO).launch {
                    unreadItemAtIndex(position, false)
                }
            }

        val view = s.view
        val tv: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    private fun markSnackbar(position: Int) {
        val s = Snackbar
            .make(
                app.findViewById(R.id.coordLayout),
                R.string.marked_as_unread,
                Snackbar.LENGTH_LONG
            )
            .setAction(R.string.undo_string) {
                readItemAtIndex(position)
            }

        val view = s.view
        val tv: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    fun handleItemAtIndex(position: Int) {
        if (items[position].unread) {
            readItemAtIndex(position)
        } else {
            unreadItemAtIndex(position)
        }
    }

    private fun readItemAtIndex(position: Int, showSnackbar: Boolean = true) {
        val i = items[position]
        CoroutineScope(Dispatchers.IO).launch {
            repository.markAsRead(i.id.toString())
            // TODO: update db

        }
        // Todo:
//        if (SharedItems.displayedItems == "unread") {
//            items.remove(i)
//            notifyItemRemoved(position)
//            updateItems(items)
//        } else {
//            notifyItemChanged(position)
//        }
        if (showSnackbar) {
            unmarkSnackbar(i, position)
        }
    }

    private fun unreadItemAtIndex(position: Int, showSnackbar: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            repository.unmarkAsRead(items[position].id.toString())
            // Todo: SharedItems.unreadItem(app, api, db, items[position])
            // TODO: update db

        }
        notifyItemChanged(position)
        if (showSnackbar) {
            markSnackbar(position)
        }
    }

    fun addItemAtIndex(item: SelfossModel.Item, position: Int) {
        items.add(position, item)
        notifyItemInserted(position)
        updateItems(items)

    }

    fun addItemsAtEnd(newItems: List<SelfossModel.Item>) {
        val oldSize = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(oldSize, newItems.size)
        updateItems(items)

    }
}