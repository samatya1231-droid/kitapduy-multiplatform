package com.semtinazizi.kitapduy.di

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.koin.core.module.Module
import org.koin.dsl.module

// Geçici iOS Dummy implementation'ları - İleride Supabase veya Firebase-iOS eklenecek
class DummyBookRepository : BookRepository {
    override suspend fun getBooks(): List<Book> = emptyList()
    override suspend fun getFavoriteBookIds(userId: String): Set<String> = emptySet()
    override suspend fun getDailySummary(): Pair<String, String>? = null
    override suspend fun getDailyQuote(): Triple<String, String, String>? = null
}

class DummyAuthRepository : AuthRepository {
    override val currentUserUid: String? = null
    override val isUserLoggedIn: Boolean = false
    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = emptyFlow()
    override suspend fun getUserProfile(uid: String): UserProfile? = null
    override suspend fun signOut() {}
}

actual val platformModule: Module = module {
    single<BookRepository> { DummyBookRepository() }
    single<AuthRepository> { DummyAuthRepository() }
}
