package com.semtinazizi.kitapduy.ui.viewmodel

import com.russhwolf.settings.Settings
import com.semtinazizi.kitapduy.domain.repository.*
import com.semtinazizi.kitapduy.model.*
import com.semtinazizi.kitapduy.platform.FileDownloader
import com.semtinazizi.kitapduy.util.KmpBookProgressManager
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.lang.reflect.Proxy
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryScreenModelTest {
    private fun progress(): KmpBookProgressManager {
        val values = mutableMapOf<String, Any>()
        val settings = Proxy.newProxyInstance(Settings::class.java.classLoader, arrayOf(Settings::class.java)) { _, method, args ->
            val key = args?.firstOrNull() as? String
            when {
                method.name.startsWith("put") -> { values[key!!] = args!![1]; Unit }
                method.name == "getKeys" -> values.keys
                method.name.startsWith("get") && key != null -> values[key] ?: args?.getOrNull(1)
                else -> null
            }
        } as Settings
        return KmpBookProgressManager(settings)
    }
    private fun auth(uid: String? = null) = Proxy.newProxyInstance(AuthRepository::class.java.classLoader,
        arrayOf(AuthRepository::class.java)) { _, method, _ ->
        if (method.name == "getCurrentUserUid") uid else null
    } as AuthRepository

    @Test fun offlineDownloadsWorkWithoutLoginOrCatalog() = runTest {
        val progress = progress().apply { saveContentMetadata("a", "Kitap", "Yazar", "kapak") }
        val repo = LibraryBooks().apply { fail = true }
        val model = LibraryScreenModel(repo, auth(), LibraryFiles(), progress, backgroundScope)
        model.loadBookshelf("DOWNLOADED")
        assertFalse(model.isLoading.value)
        val item = model.bookshelfItems.value.single()
        assertEquals("Kitap", item.bookTitle)
        assertEquals("file:///a.mp3", model.bookFor(item)?.audioUrl)
        runCurrent()
        assertNull(model.errorMessage.value)
        assertEquals(0, repo.shelfCalls)
        assertEquals(1, model.bookshelfItems.value.size)
    }

    @Test fun historyRequiresActualProgressAndIncludesLegacyCatalogRecords() = runTest {
        val progress = progress().apply {
            saveContentMetadata("a", "Kitap", "Yazar", "")
            saveContentMetadata("downloadOnly", "İndirme", "", "")
            saveProgress("a", 100)
            saveProgress("legacy", 200)
        }
        val repo = LibraryBooks().apply { catalog = listOf(Book(id = "legacy", title = "Eski")) }
        val model = LibraryScreenModel(repo, auth(), LibraryFiles(), progress, backgroundScope)
        model.loadBookshelf("LISTENING"); runCurrent()
        assertEquals(setOf("a", "legacy"), model.bookshelfItems.value.map { it.bookId }.toSet())
        assertEquals(0, repo.shelfCalls)
    }

    @Test fun switchingTabsCancelsOldShelfResponse() = runTest {
        val repo = LibraryBooks().apply { gate = CompletableDeferred() }
        val model = LibraryScreenModel(repo, auth("uid"), LibraryFiles(), progress(), backgroundScope)
        model.loadBookshelf("FINISHED"); runCurrent()
        model.loadBookshelf("DOWNLOADED"); runCurrent()
        repo.gate!!.complete(Unit); runCurrent()
        assertEquals("a", model.bookshelfItems.value.single().bookId)
        assertEquals("DOWNLOADED", model.bookshelfItems.value.single().shelfType)
        assertFalse(model.isLoading.value)
    }

    @Test fun serverFailureIsNotAnEmptyShelfAndRetryRecovers() = runTest {
        val repo = LibraryBooks().apply { fail = true }
        val model = LibraryScreenModel(repo, auth("uid"), LibraryFiles(), progress(), backgroundScope)
        model.loadBookshelf("FINISHED"); runCurrent()
        assertNotNull(model.errorMessage.value)
        assertFalse(model.isLoading.value)
        repo.fail = false
        model.loadBookshelf("FINISHED"); runCurrent()
        assertNull(model.errorMessage.value)
        assertEquals("server", model.bookshelfItems.value.single().bookId)
    }

    @Test fun favoritesLoadFromRepositoryProperly() = runTest {
        val repo = LibraryBooks()
        val model = LibraryScreenModel(repo, auth("uid"), LibraryFiles(), progress(), backgroundScope)
        model.loadBookshelf("FAVORITE"); runCurrent()
        assertNull(model.errorMessage.value)
        assertEquals(1, repo.shelfCalls)
        assertEquals("server", model.bookshelfItems.value.single().bookId)
        assertEquals("FAVORITE", model.bookshelfItems.value.single().shelfType)
    }
}

private class LibraryFiles : FileDownloader {
    override suspend fun downloadFile(url: String, fileName: String, onProgress: (Float) -> Unit) = error("Unused")
    override fun getDownloadedBookIds() = listOf("a")
    override fun getDownloadedFilePath(fileName: String) = if (fileName == "a.mp3") "file:///a.mp3" else null
    override fun deleteFile(fileName: String) = false
}

private class LibraryBooks : BookRepository {
    var fail = false
    var shelfCalls = 0
    var gate: CompletableDeferred<Unit>? = null
    var catalog = emptyList<Book>()
    override suspend fun getBooks(): List<Book> { if (fail) error("Offline"); return catalog }
    override suspend fun getBookshelf(userId: String, shelfType: String): List<SupabaseBookshelf> {
        shelfCalls++
        gate?.await()
        if (fail) error("Offline")
        return listOf(SupabaseBookshelf(userId = userId, bookId = "server", shelfType = shelfType))
    }
    override suspend fun getFavoriteBookIds(userId: String) = emptySet<String>()
    override suspend fun getDailySummary(): Pair<String, String>? = null
    override suspend fun getDailyQuote(): Triple<String, String, String>? = null
    override suspend fun searchBooks(query: String) = emptyList<Book>()
    override suspend fun getPodcasts() = emptyList<Podcast>()
    override suspend fun searchPodcasts(query: String) = emptyList<Podcast>()
}
