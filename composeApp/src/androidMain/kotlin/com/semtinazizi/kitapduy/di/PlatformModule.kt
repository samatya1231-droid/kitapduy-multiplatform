package com.semtinazizi.kitapduy.di

import android.app.Activity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.semtinazizi.kitapduy.data.repository.FirebaseAuthRepository
import com.semtinazizi.kitapduy.data.repository.FirebaseBookRepository
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.platform.AdManager
import com.semtinazizi.kitapduy.platform.BillingManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { Firebase.firestore }
    single { Firebase.auth }

    single<BookRepository> { FirebaseBookRepository(get()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }

    // AdMob ve Billing — Activity context ile
    // Not: Activity scope'u için Koin'in activityScope kullanılabilir;
    // basitlik adına androidContext() ile Activity cast ediyoruz.
    single {
        AdManager(androidContext() as? Activity ?: error("AdManager için Activity context gerekli"))
    }
    single {
        BillingManager(androidContext() as? Activity ?: error("BillingManager için Activity context gerekli"))
    }
}

