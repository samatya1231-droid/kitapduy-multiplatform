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

    // Gerçek Supabase Repositorileri
    single<com.semtinazizi.kitapduy.domain.repository.BookRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseBookRepository(get()) 
    }
    single<com.semtinazizi.kitapduy.domain.repository.AuthRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseAuthRepository(get()) 
    }
}

val appModules = listOf(
    coreModule,
    platformModule,
    viewModelModule
)
