package com.semtinazizi.kitapduy.di

import com.semtinazizi.kitapduy.data.SupabaseManager
import com.semtinazizi.kitapduy.platform.AppConfig
import org.koin.dsl.module

/**
 * Koin DI Modülleri
 * Hilt'in KMP karşılığı - hem Android hem iOS'ta çalışır
 */

/** Temel bağımlılıklar: Supabase, config */
val coreModule = module {
    single {
        SupabaseManager.apply {
            initialize(
                supabaseUrl = AppConfig.supabaseUrl,
                supabaseKey = AppConfig.supabaseKey
            )
        }.client
    }
}

val appModules = listOf(
    coreModule,
    platformModule,
    viewModelModule
)
