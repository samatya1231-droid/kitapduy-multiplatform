package com.semtinazizi.kitapduy

import android.app.Application
import com.semtinazizi.kitapduy.platform.BuildConfigHelper
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.semtinazizi.kitapduy.di.appModules

class KitapDuyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.semtinazizi.kitapduy.platform.androidAppContext = applicationContext

        // BuildConfig değerlerini platform helper'a aktar
        BuildConfigHelper.supabaseUrl = BuildConfig.SUPABASE_URL
        BuildConfigHelper.supabaseKey = BuildConfig.SUPABASE_KEY
        BuildConfigHelper.cfAccountId = BuildConfig.CF_ACCOUNT_ID
        BuildConfigHelper.cfPublicUrl = BuildConfig.CF_PUBLIC_URL

        // Koin DI başlat
        startKoin {
            androidContext(this@KitapDuyApp)
            modules(appModules)
        }
        
        // OneSignal'ı başlat (Android Context geçerek)
        com.semtinazizi.kitapduy.platform.PushNotificationManager.init(this)
    }
}
