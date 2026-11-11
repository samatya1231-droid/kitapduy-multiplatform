package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.model.Book
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import com.semtinazizi.kitapduy.platform.FileDownloader
import com.semtinazizi.kitapduy.util.KmpBookProgressManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LibraryScreenModel(
    private val bookRepository: BookRepository,
    private val authRepository: AuthRepository,
    private val downloader: FileDownloader,
    private val progress: KmpBookProgressManager,
    private val suppliedScope: CoroutineScope? = null
) : ScreenModel {
    private val scope get() = suppliedScope ?: screenModelScope
    private var loadJob: Job? = null
    private var books = emptyMap<String, Book>()
    private val _bookshelfItems = MutableStateFlow<List<SupabaseBookshelf>>(emptyList())
    val bookshelfItems = _bookshelfItems.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun bookFor(item: SupabaseBookshelf): Book? {
        val local = downloader.getDownloadedFilePath(item.bookId + ".mp3")
        val book = books[item.bookId]
        if (book != null) return if (local != null) book.copy(audioUrl = local) else book
        if (local == null) {
            _errorMessage.value = "Kitap bilgileri yüklenemedi. İnternet bağlantınızı kontrol edip tekrar deneyin."
            return null
        }
        val metadata = progress.getContentMetadata(item.bookId)
        return Book(id = item.bookId, title = item.bookTitle.orEmpty(), author = metadata?.second.orEmpty(),
            coverUrl = item.bookCoverUrl.orEmpty(), audioUrl = local)
    }

    fun loadBookshelf(shelfType: String) {
        loadJob?.cancel()
        _bookshelfItems.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
        val localShelf = shelfType == "LISTENING" || shelfType == "DOWNLOADED"
        val uid = authRepository.currentUserUid
        if (!localShelf && uid == null) {
            _errorMessage.value = "Lütfen giriş yapın."
            return
        }
        fun localItems(): List<SupabaseBookshelf> {
            val ids = if (shelfType == "DOWNLOADED") downloader.getDownloadedBookIds()
                else (progress.getHistoryIds() + books.keys.filter { progress.getProgress(it) > 0 }).distinct()
            return ids.map { id ->
                val book = books[id]
                val metadata = progress.getContentMetadata(id)
                SupabaseBookshelf(userId = uid.orEmpty(), bookId = id, shelfType = shelfType,
                    bookTitle = book?.title ?: metadata?.first ?: id,
                    bookCoverUrl = book?.coverUrl ?: metadata?.third)
            }
        }
        if (localShelf) _bookshelfItems.value = localItems()
        _isLoading.value = !localShelf
        loadJob = scope.launch {
            try {
                if (!localShelf) {
                    val items = if (shelfType == "FAVORITE") {
                        val favIds = runCatching { bookRepository.getFavoriteBookIds(uid!!) }.getOrDefault(emptySet())
                        val shelfItems = runCatching { bookRepository.getBookshelf(uid!!, shelfType) }.getOrDefault(emptyList())
                        val allFavIds = (favIds + shelfItems.map { it.bookId }).distinct()
                        if (shelfItems.isNotEmpty() && favIds.isEmpty()) {
                            shelfItems
                        } else {
                            allFavIds.map { bookId ->
                                shelfItems.find { it.bookId == bookId } ?: SupabaseBookshelf(
                                    userId = uid!!,
                                    bookId = bookId,
                                    shelfType = "FAVORITE"
                                )
                            }
                        }
                    } else {
                        bookRepository.getBookshelf(uid!!, shelfType)
                    }
                    ensureActive()
                    _bookshelfItems.value = items.distinctBy { it.bookId }
                }
                // Device downloads remain usable immediately, even when the catalog is offline.
                val catalog = withTimeoutOrNull(8_000) { bookRepository.getBooks() }
                ensureActive()
                if (catalog != null) books = catalog.filter { it.id.isNotBlank() && it.id != "error" }.associateBy { it.id }
                if (!localShelf) {
                    _bookshelfItems.value = _bookshelfItems.value.map { item ->
                        val b = books[item.bookId]
                        if (b != null) {
                            item.copy(
                                bookTitle = if (item.bookTitle.isNullOrBlank()) b.title else item.bookTitle,
                                bookCoverUrl = if (item.bookCoverUrl.isNullOrBlank()) b.coverUrl else item.bookCoverUrl
                            )
                        } else item
                    }
                }
                if (localShelf) _bookshelfItems.value = localItems()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                if (!localShelf) _errorMessage.value = "Kitaplık yüklenemedi. Lütfen tekrar deneyin."
            } finally {
                if (isActive) _isLoading.value = false
            }
        }
    }
}
