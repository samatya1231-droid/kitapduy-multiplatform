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
    override suspend fun searchBooks(query: String): List<Book> = emptyList()
    override suspend fun getBookshelf(userId: String, shelfType: String): List<com.semtinazizi.kitapduy.model.SupabaseBookshelf> = emptyList()
    override suspend fun getPodcasts(): List<com.semtinazizi.kitapduy.model.Podcast> = emptyList()
    override suspend fun searchPodcasts(query: String): List<com.semtinazizi.kitapduy.model.Podcast> = emptyList()
}

class DummyAuthRepository : AuthRepository {
    override val currentUserUid: String? = null
    override val isUserLoggedIn: Boolean = false
    override fun getUserProfileFlow(uid: String): Flow<UserProfile?> = emptyFlow()
    override suspend fun getUserProfile(uid: String): UserProfile? = null
    override suspend fun signInWithEmail(email: String, password: String): Boolean = false
    override suspend fun signUpWithEmail(email: String, password: String, name: String): Boolean = false
    override suspend fun updateProfile(uid: String, name: String, bio: String, username: String?): Boolean = true
    override suspend fun signOut() {}
    override suspend fun awaitSessionInitialization() {}
    override suspend fun signInWithGoogle(): Boolean = false
    override suspend fun signInWithApple(): Boolean = false
    override suspend fun getGoogleOAuthUrl(): String = ""
    override suspend fun getAppleOAuthUrl(): String = ""
    override suspend fun updateProfilePhoto(uid: String, photoUrl: String?): Boolean = true
}

actual val platformModule: Module = module {

    
    single<com.semtinazizi.kitapduy.platform.AudioPlayer> { 
        com.semtinazizi.kitapduy.platform.createAudioPlayer() 
    }
}
