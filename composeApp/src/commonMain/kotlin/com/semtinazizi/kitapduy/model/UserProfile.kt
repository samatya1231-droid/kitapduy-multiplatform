package com.semtinazizi.kitapduy.model

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val coverPhotoUrl: String? = null,
    val badges: List<String> = emptyList(),
    val myReferralCode: String = "",
    val referredBy: String? = null,
    val premiumUntil: Long = 0L,
    val successfulInvites: Int = 0,
    val bio: String = "",
    val username: String? = null,
    val streak: Int = 0,
    val isPremium: Boolean = false
)
