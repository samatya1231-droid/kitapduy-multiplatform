package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.BookRepository
import com.semtinazizi.kitapduy.domain.repository.SocialRepository
import com.semtinazizi.kitapduy.model.FeedItem
import com.semtinazizi.kitapduy.model.SupabaseBookshelf
import com.semtinazizi.kitapduy.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileScreenModel(
    private val authRepository: AuthRepository,
    private val socialRepository: SocialRepository,
    private val bookRepository: BookRepository
) : ScreenModel {

    private val _targetUserProfile = MutableStateFlow<UserProfile?>(null)
    val targetUserProfile: StateFlow<UserProfile?> = _targetUserProfile.asStateFlow()

    private val _userPosts = MutableStateFlow<List<FeedItem>>(emptyList())
    val userPosts: StateFlow<List<FeedItem>> = _userPosts.asStateFlow()

    private val _userShelves = MutableStateFlow<List<SupabaseBookshelf>>(emptyList())
    val userShelves: StateFlow<List<SupabaseBookshelf>> = _userShelves.asStateFlow()

    private val _favoriteAuthors = MutableStateFlow<List<Pair<String, String?>>>(emptyList())
    val favoriteAuthors: StateFlow<List<Pair<String, String?>>> = _favoriteAuthors.asStateFlow()

    private val _readingDnaCategories = MutableStateFlow<Map<String, Int>>(emptyMap())
    val readingDnaCategories: StateFlow<Map<String, Int>> = _readingDnaCategories.asStateFlow()

    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount.asStateFlow()

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()
    
    private val _followListUsers = MutableStateFlow<List<Map<String, String>>>(emptyList())
    val followListUsers: StateFlow<List<Map<String, String>>> = _followListUsers.asStateFlow()

    fun loadProfileData(targetUserId: String?) {
        val uid = targetUserId ?: authRepository.currentUserUid ?: return
        
        screenModelScope.launch {
            _targetUserProfile.value = socialRepository.getUserProfile(uid)
            
            _userPosts.value = socialRepository.fetchUserPosts(uid)
            
            val shelves = socialRepository.fetchUserShelves(uid).distinctBy { it.bookId }
            _userShelves.value = shelves
            
            val allBooks = bookRepository.getBooks()
            calculateFavoriteAuthors(shelves, allBooks)
            calculateReadingDna(shelves, allBooks)
            
            loadFollowStats(uid)
            
            if (targetUserId != null && targetUserId != authRepository.currentUserUid) {
                _isFollowing.value = socialRepository.isFollowing(
                    authRepository.currentUserUid ?: "", targetUserId
                )
            }
        }
    }

    private fun loadFollowStats(userId: String) {
        screenModelScope.launch {
            _followersCount.value = socialRepository.getFollowers(userId).size
            _followingCount.value = socialRepository.getFollowing(userId).size
        }
    }

    fun toggleFollow(targetUserId: String) {
        val currentUid = authRepository.currentUserUid ?: return
        if (currentUid == targetUserId) return
        
        screenModelScope.launch {
            val alreadyFollowing = socialRepository.isFollowing(currentUid, targetUserId)
            if (alreadyFollowing) {
                socialRepository.unfollow(currentUid, targetUserId)
                _isFollowing.value = false
            } else {
                socialRepository.follow(currentUid, targetUserId)
                _isFollowing.value = true
                val myProfile = authRepository.getUserProfile(currentUid)
                socialRepository.sendNotification(
                    receiverId = targetUserId,
                    message = "${myProfile?.name ?: "Birisi"} seni takip etmeye başladı! \uD83D\uDC64",
                    type = "follow"
                )
            }
            loadFollowStats(targetUserId)
        }
    }

    fun fetchFollowList(userId: String, isFollowers: Boolean) {
        screenModelScope.launch {
            val records = if (isFollowers) {
                socialRepository.getFollowers(userId)
            } else {
                socialRepository.getFollowing(userId)
            }
            
            val userList = mutableListOf<Map<String, String>>()
            for (record in records) {
                val targetUid = if (isFollowers) record.followerId else record.followingId
                val profile = socialRepository.getUserProfile(targetUid)
                if (profile != null) {
                    userList.add(
                        mapOf(
                            "uid" to targetUid,
                            "name" to profile.name,
                            "photo" to (profile.photoUrl ?: "")
                        )
                    )
                }
            }
            _followListUsers.value = userList
        }
    }

    fun updateUserBio(bio: String) {
        val uid = authRepository.currentUserUid ?: return
        screenModelScope.launch {
            val success = authRepository.updateProfile(uid, name = _targetUserProfile.value?.name ?: "", bio = bio, username = _targetUserProfile.value?.username)
            if (success) {
                _targetUserProfile.value = authRepository.getUserProfile(uid)
            }
        }
    }

    private fun calculateFavoriteAuthors(shelves: List<SupabaseBookshelf>, allBooks: List<com.semtinazizi.kitapduy.model.Book>) {
        val authorFrequency = shelves
            .mapNotNull { shelf -> allBooks.find { it.id == shelf.bookId }?.author }
            .filter { it.isNotBlank() && it != "Bilinmeyen Yazar" }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(10)

        val authorImages = mapOf(
            "Sabahattin Ali" to "https://upload.wikimedia.org/wikipedia/commons/4/4e/Sabahattin_Ali_Profile.jpg",
            "Franz Kafka" to "https://upload.wikimedia.org/wikipedia/commons/4/4c/Kafka.jpg",
            "Stefan Zweig" to "https://upload.wikimedia.org/wikipedia/commons/d/d4/Stefan_Zweig_1912.jpg",
            "Virginia Woolf" to "https://upload.wikimedia.org/wikipedia/commons/0/0b/Virginia_Woolf_1927.jpg",
            "Lev Tolstoy" to "https://upload.wikimedia.org/wikipedia/commons/c/c6/L.N.Tolstoy_GK.jpg",
            "Fyodor Dostoyevski" to "https://upload.wikimedia.org/wikipedia/commons/7/78/Dostoevsky_1872.jpg",
            "George Orwell" to "https://upload.wikimedia.org/wikipedia/commons/7/7e/George_Orwell_press_photo.jpg",
            "Albert Camus" to "https://upload.wikimedia.org/wikipedia/commons/0/08/Albert_Camus%2C_en_frock_coat%2C_portrait%2C_sitting%2C_facing_left%2C_seven-quarter_length.jpg",
            "Oğuz Atay" to "https://upload.wikimedia.org/wikipedia/tr/b/b3/Oğuz_Atay.jpg",
            "Ahmet Hamdi Tanpınar" to "https://upload.wikimedia.org/wikipedia/tr/b/b0/Ahmet_Hamdi_Tanpınar.jpg",
            "Sait Faik Abasıyanık" to "https://upload.wikimedia.org/wikipedia/tr/3/3d/Sait_Faik_Abasıyanık.jpg",
            "Cemal Süreya" to "https://upload.wikimedia.org/wikipedia/tr/d/d1/Cemal_Süreya.jpg",
            "Nazım Hikmet" to "https://upload.wikimedia.org/wikipedia/commons/e/e0/Nazim_Hikmet_1950.jpg",
            "Gabriel García Márquez" to "https://upload.wikimedia.org/wikipedia/commons/0/0f/Gabriel_Garcia_Marquez.jpg",
            "José Saramago" to "https://upload.wikimedia.org/wikipedia/commons/2/2c/José_Saramago_from_the_Nobel_Foundation_archive.jpg",
            "Haruki Murakami" to "https://upload.wikimedia.org/wikipedia/commons/1/1a/Haruki_Murakami_%282009%29.jpg",
            "Victor Hugo" to "https://upload.wikimedia.org/wikipedia/commons/e/e6/Victor_Hugo_by_Étienne_Carjat_1876_-_full_lengh.jpg",
            "Charles Dickens" to "https://upload.wikimedia.org/wikipedia/commons/a/aa/Dickens_Gurney_head.jpg",
            "Jane Austen" to "https://upload.wikimedia.org/wikipedia/commons/c/cc/CassandraAusten-JaneAusten%28c.1810%29_rotated.jpg"
        )

        _favoriteAuthors.value = authorFrequency.map { it.first to authorImages[it.first] }
    }

    private fun calculateReadingDna(shelves: List<SupabaseBookshelf>, allBooks: List<com.semtinazizi.kitapduy.model.Book>) {
        val categoryFrequency = shelves
            .mapNotNull { shelf -> allBooks.find { it.id == shelf.bookId }?.category }
            .filter { it.isNotBlank() && it.lowercase() != "bilinmiyor" }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()

        _readingDnaCategories.value = categoryFrequency
    }
}
