package bou.amine.apps.readerforselfossv2.DI

import bou.amine.apps.readerforselfossv2.rest.SelfossApi
import bou.amine.apps.readerforselfossv2.rest.SelfossApiImpl
import bou.amine.apps.readerforselfossv2.service.ApiDetailsService
import bou.amine.apps.readerforselfossv2.service.ApiDetailsServiceImpl
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

val networkModule by DI.Module {
    bind<ApiDetailsService>() with singleton { ApiDetailsServiceImpl() }
    bind<SelfossApi>() with singleton { SelfossApiImpl(instance()) }
}