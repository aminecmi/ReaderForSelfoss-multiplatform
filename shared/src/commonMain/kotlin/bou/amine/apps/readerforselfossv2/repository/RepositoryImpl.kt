package bou.amine.apps.readerforselfossv2.repository

import bou.amine.apps.readerforselfossv2.rest.SelfossApi
import bou.amine.apps.readerforselfossv2.rest.SelfossModel
import bou.amine.apps.readerforselfossv2.service.ApiDetailsService
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RepositoryImpl(private val api: SelfossApi, private val apiDetails: ApiDetailsService) : Repository {
    val settings = Settings()

    override lateinit var items: List<SelfossModel.Item>
    override lateinit var selectedItems: List<SelfossModel.Item>
    override var baseUrl = apiDetails.getBaseUrl()

    override var apiMajorVersion = 0

    init {
        // TODO: Dispatchers.IO not available in KMM, an alternative solution should be found
        CoroutineScope(Dispatchers.Main).launch {
            updateApiVersion()
        }
    }

    override fun getMoreItems(): List<SelfossModel.Item> {
        TODO("Not yet implemented")
    }

    override fun stats(): SelfossModel.Stats {
        TODO("Not yet implemented")
    }

    override fun getTags(): List<SelfossModel.Tag> {
        TODO("Not yet implemented")
    }

    override suspend fun getSpouts(): Map<String, SelfossModel.Spout>? {
        // TODO: Check success, store in DB
        return api.spouts()
    }

    override suspend fun getSources(): ArrayList<SelfossModel.Source>? {
        // TODO: Check success
        return api.sources()
    }

    override suspend fun markAsRead(id: String): Boolean {
        // TODO: Check success, store in DB
        api.markAsRead(id)
        return true
    }

    override suspend fun unmarkAsRead(id: String): Boolean {
        // TODO: Check success, store in DB
        api.unmarkAsRead(id)
        return true    }

    override suspend fun starr(id: String): Boolean {
        // TODO: Check success, store in DB
        api.starr(id)
        return true
    }

    override suspend fun unstarr(id: String): Boolean {
        // TODO: Check success, store in DB
        api.unstarr(id)
        return true
    }

    override fun markAllAsRead(ids: List<String>): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createSource(
        title: String,
        url: String,
        spout: String,
        tags: String,
        filter: String
    ): Boolean {
        // TODO: Check connectivity
        var result = false
        val response = api.createSourceForVersion(
            title,
            url,
            spout,
            tags,
            filter,
            apiMajorVersion
        )

        if (response != null) {
            result = true
        }

        return result
    }

    override suspend fun deleteSource(id: Int): Boolean {
        // TODO: Check connectivity, store in DB
        var success = false
        val response = api.deleteSource(id)
        if (response != null) {
            success = response.isSuccess
        }

        return success
    }

    override fun updateRemote(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun login(): Boolean {
        var result = false
        try {
            val response = api.login()
            if (response != null && response.isSuccess) {
                result = true
            }
        } catch (cause: Throwable) {
            Napier.e(cause.message!!, tag = "1")
            Napier.e(cause.stackTraceToString(),tag = "1")
        }
        return result
    }

    override fun refreshLoginInformation() {
        api.refreshLoginInformation()
        baseUrl = apiDetails.getBaseUrl()
    }

    private suspend fun updateApiVersion() {
        // TODO: Handle connectivity issues
        val fetchedVersion = api.version()
        if (fetchedVersion != null) {
            apiMajorVersion = fetchedVersion.getApiMajorVersion()
            settings.putInt("apiVersionMajor", apiMajorVersion)
        } else {
            apiMajorVersion = settings.getInt("apiVersionMajor", 0)
        }
    }
}