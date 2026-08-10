package com.semtinazizi.kitapduy.di

import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.semtinazizi.kitapduy.data.repository.FirebaseAuthRepository
import com.semtinazizi.kitapduy.data.repository.FirebaseBookRepository
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { Firebase.firestore }
    single { Firebase.auth }

    single<BookRepository> { FirebaseBookRepository(get()) }
    single<AuthRepository> { FirebaseAuthRepository(get(), get()) }
}
