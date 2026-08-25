package com.semtinazizi.kitapduy.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.semtinazizi.kitapduy.model.QuizQuestion

@Serializable
data class QuizLeaderboardEntry(
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String = "Anonim",
    @SerialName("total_score") val totalScore: Long = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class QuizMatch(
    @SerialName("match_id") val matchId: String = "",
    @SerialName("room_code") val roomCode: String? = null,
    @SerialName("player1_id") val player1Id: String? = null,
    @SerialName("player1_name") val player1Name: String? = null,
    @SerialName("player2_id") val player2Id: String? = null,
    @SerialName("player2_name") val player2Name: String? = null,
    @SerialName("status") val status: String = "WAITING", // WAITING, WAITING_PRIVATE, PLAYING, FINISHED
    @SerialName("current_turn_id") val currentTurnId: String? = null,
    @SerialName("player1_score") val player1Score: Int = 0,
    @SerialName("player2_score") val player2Score: Int = 0,
    @SerialName("player1_progress") val player1Progress: Int = 0,
    @SerialName("player2_progress") val player2Progress: Int = 0,
    @SerialName("match_questions") val matchQuestions: List<QuizQuestion> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null
)