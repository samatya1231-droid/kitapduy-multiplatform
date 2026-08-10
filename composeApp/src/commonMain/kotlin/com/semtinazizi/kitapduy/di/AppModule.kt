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

    // Geçici olarak UI testleri için Dummy Repositoriler
    single<com.semtinazizi.kitapduy.domain.repository.BookRepository> { 
        com.semtinazizi.kitapduy.data.repository.DummyBookRepository() 
    }
    single<com.semtinazizi.kitapduy.domain.repository.AuthRepository> { 
        com.semtinazizi.kitapduy.data.repository.DummyAuthRepository() 
    }
}

val appModules = listOf(
    coreModule,
    platformModule,
    viewModelModule
)
