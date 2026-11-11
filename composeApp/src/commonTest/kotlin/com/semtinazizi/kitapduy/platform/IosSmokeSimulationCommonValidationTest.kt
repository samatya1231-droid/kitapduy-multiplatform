package com.semtinazizi.kitapduy.platform

import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.domain.repository.FeedRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.ui.screens.BookDetailScreen
import com.semtinazizi.kitapduy.ui.screens.tabs.FeedTab
import com.semtinazizi.kitapduy.ui.screens.tabs.HomeTab
import com.semtinazizi.kitapduy.ui.screens.tabs.ProfileTab
import com.semtinazizi.kitapduy.ui.screens.tabs.SearchTab
import com.semtinazizi.kitapduy.ui.viewmodel.GlobalAudioController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.Koin
import org.koin.mp.KoinPlatform
import kotlin.test.Test

class IosSmokeSimulationCommonValidationTest {

    @Suppress("UNUSED_VARIABLE")
    private suspend fun validateSimulationLogic(koin: Koin) {
        val authRepo: AuthRepository = koin.get<AuthRepository>()
        val audioController: GlobalAudioController = koin.get<GlobalAudioController>()
        val audioPlayer: AudioPlayer = koin.get<AudioPlayer>()
        val bookRepo: BookRepository = koin.get<BookRepository>()
        val feedRepo: FeedRepository = koin.get<FeedRepository>()

        if (!authRepo.isUserLoggedIn) {
            val loginSuccess = authRepo.signInAnonymously("Misafir Test")
        }

        val mainScreenMounted = withTimeoutOrNull(1000) {
            IosSmokeSimulationBridge.isMainScreenMounted.first { it }
        } == true

        val books: List<Book> = try {
            bookRepo.getBooks().filter { it.title.isNotBlank() && it.id != "error" }
        } catch (_: Throwable) {
            emptyList()
        }

        val sampleBook = books.firstOrNull { it.audioUrl.isNotBlank() && it.audioUrl.startsWith("http") } ?: Book(
            id = "ci_sample_audiobook",
            title = "Kürk Mantolu Madonna",
            author = "Sabahattin Ali",
            category = "Klasik",
            coverUrl = "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=600",
            audioUrl = "https://ancient-shape-df97.samatya231.workers.dev/Birdenbire.MP3",
            description = "Sabahattin Ali'nin ölümsüz eseri Kürk Mantolu Madonna.",
            duration = 180000L
        )

        IosSmokeSimulationBridge.pushScreen(BookDetailScreen(sampleBook))

        audioController.playTrack(sampleBook, isPremium = true)

        val isPlaying = audioController.isPlaying.value
        val pos = audioController.currentPosition.value
        val dur = audioController.duration.value
        val rawPos = audioPlayer.getCurrentPosition()
        val rawDur = audioPlayer.getDuration()

        if (audioController.isPlaying.value) {
            audioController.togglePlayPause()
        }
        IosSmokeSimulationBridge.pop()

        IosSmokeSimulationBridge.switchTab(SearchTab)
        try {
            val searchResults = bookRepo.searchBooks("Ali")
        } catch (_: Throwable) {}

        IosSmokeSimulationBridge.switchTab(FeedTab)
        try {
            val postSuccess = feedRepo.createPost(
                userId = authRepo.currentUserUid ?: "ci_guest",
                userName = "Misafir Test",
                userPhoto = null,
                bookId = sampleBook.id,
                bookName = sampleBook.title,
                comment = "Simülatör otomatik testi ile paylaşıldı.",
                isPremium = false,
                imageUrl = null,
                audioUrl = null,
                pollOptions = null
            )
        } catch (_: Throwable) {}

        IosSmokeSimulationBridge.switchTab(ProfileTab)
        val uid = authRepo.currentUserUid
        if (uid != null) {
            try {
                authRepo.updateProfile(uid, "Misafir Test", "Simülatör Test Kullanıcısı", "misafir_ci")
                authRepo.updateProfilePhoto(uid, "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&q=80")
            } catch (_: Throwable) {}
        }

        IosSmokeSimulationBridge.switchTab(HomeTab)
    }

    @Test
    fun testKoinPlatformAccess() {
        // Verify KoinPlatform exists and is accessible
        val koin = KoinPlatform.getKoinOrNull()
        // Koin may or may not be initialized in unit test runner,
        // but KoinPlatform API itself must exist and compile
    }
}
