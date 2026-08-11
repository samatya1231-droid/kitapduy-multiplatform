package com.semtinazizi.kitapduy.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("cover_photo_url") val coverPhotoUrl: String? = null,
    val badges: List<String> = emptyList(),
    @SerialName("my_referral_code") val myReferralCode: String = "",
    @SerialName("referred_by") val referredBy: String? = null,
    @SerialName("premium_until") val premiumUntil: Long = 0L,
    @SerialName("successful_invites") val successfulInvites: Int = 0,
    val bio: String = "",
    val username: String? = null,
    val streak: Int = 0,
    @SerialName("is_premium") val isPremium: Boolean = false
)
