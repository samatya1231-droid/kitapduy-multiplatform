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
    single<com.semtinazizi.kitapduy.domain.repository.AuthRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseAuthRepository(get()) 
    }
    single<com.semtinazizi.kitapduy.domain.repository.BookRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseBookRepository(get()) 
    }
    single<com.semtinazizi.kitapduy.domain.repository.FeedRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseFeedRepository(get()) 
    }
    single<com.semtinazizi.kitapduy.domain.repository.SocialRepository> { 
        com.semtinazizi.kitapduy.data.repository.SupabaseSocialRepository(get(), get()) 
    }
    single { io.ktor.client.HttpClient() }

    single<com.semtinazizi.kitapduy.data.repository.StorageRepository> {
        com.semtinazizi.kitapduy.data.repository.R2StorageRepository(get())
    }

    single { com.semtinazizi.kitapduy.util.KmpBookProgressManager() }
    
    single<com.semtinazizi.kitapduy.platform.FileDownloader> { com.semtinazizi.kitapduy.platform.createFileDownloader() }
    single { com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController(get(), get()) }
}

val appModules = listOf(
    coreModule,
    platformModule,
    viewModelModule
)
