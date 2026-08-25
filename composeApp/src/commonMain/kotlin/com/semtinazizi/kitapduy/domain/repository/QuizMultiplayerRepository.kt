package com.semtinazizi.kitapduy.domain.repository

import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.data.model.QuizMatch
import kotlinx.coroutines.flow.Flow

interface QuizMultiplayerRepository {
    suspend fun getLeaderboard(): List<QuizLeaderboardEntry>
    suspend fun updateLeaderboard(userId: String, userName: String, scoreToAdd: Long)
    
    suspend fun createMatch(userId: String, userName: String, roomCode: String? = null): QuizMatch?
    suspend fun joinMatch(matchId: String, userId: String, userName: String): QuizMatch?
    suspend fun findWaitingMatch(): QuizMatch?
    
    suspend fun updateMatchProgress(matchId: String, isPlayer1: Boolean, progress: Int, score: Int)
    suspend fun finishMatch(matchId: String)
    
    fun observeMatch(matchId: String): Flow<QuizMatch>
}
