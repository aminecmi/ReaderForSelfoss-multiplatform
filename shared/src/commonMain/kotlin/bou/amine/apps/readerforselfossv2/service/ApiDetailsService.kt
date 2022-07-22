package bou.amine.apps.readerforselfossv2.service

interface ApiDetailsService {
    fun logApiCalls(message: String)
    fun getApiVersion(): Int
    fun getBaseUrl(): String
    fun getUserName(): String
    fun getPassword(): String
    fun refresh()
}