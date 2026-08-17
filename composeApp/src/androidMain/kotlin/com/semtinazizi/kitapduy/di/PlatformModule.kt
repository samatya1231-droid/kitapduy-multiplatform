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


    single<com.semtinazizi.kitapduy.platform.AudioPlayer> { 
        (com.semtinazizi.kitapduy.platform.createAudioPlayer() as com.semtinazizi.kitapduy.platform.AndroidAudioPlayer).apply {
            setContext(androidContext())
        }
    }

    // AdMob ve Billing — Activity context ile
    single {
        AdManager(com.semtinazizi.kitapduy.MainActivity.instance ?: error("AdManager için Activity context gerekli"))
    }
    single {
        BillingManager(com.semtinazizi.kitapduy.MainActivity.instance ?: error("BillingManager için Activity context gerekli"))
    }
}

