package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.data.model.QuizMatch
import com.semtinazizi.kitapduy.domain.repository.QuizMultiplayerRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseQuizMultiplayerRepository(
    private val supabaseClient: SupabaseClient
) : QuizMultiplayerRepository {

    override suspend fun getLeaderboard(): List<QuizLeaderboardEntry> {
        return try {
            supabaseClient.postgrest["quiz_leaderboard"]
                .select {
                    order("total_score", Order.DESCENDING)
                    limit(100)
                }
                .decodeList<QuizLeaderboardEntry>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun updateLeaderboard(userId: String, userName: String, scoreToAdd: Long) {
        try {
            // First try to fetch the current score
            val existing = supabaseClient.postgrest["quiz_leaderboard"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<QuizLeaderboardEntry>()
                
            val newScore = (existing?.totalScore ?: 0) + scoreToAdd
            
            val newEntry = QuizLeaderboardEntry(
                userId = userId,
                userName = userName,
                totalScore = newScore
            )
            
            supabaseClient.postgrest["quiz_leaderboard"].upsert(newEntry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun createMatch(userId: String, userName: String, roomCode: String?): QuizMatch? {
        return try {
            val status = if (roomCode != null) "WAITING_PRIVATE" else "WAITING"
            val newMatch = QuizMatch(
                player1Id = userId,
                player1Name = userName,
                roomCode = roomCode,
                status = status
            )
            
            supabaseClient.postgrest["quiz_matches"]
                .insert(newMatch) { select() }
                .decodeSingle<QuizMatch>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun joinMatch(matchId: String, userId: String, userName: String): QuizMatch? {
        return try {
            val update = buildJsonObject {
                put("player2_id", userId)
                put("player2_name", userName)
                put("status", "PLAYING")
            }
            
            supabaseClient.postgrest["quiz_matches"]
                .update(update) {
                    filter { eq("match_id", matchId) }
                    select()
                }
                .decodeSingle<QuizMatch>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun findWaitingMatch(): QuizMatch? {
        return try {
            supabaseClient.postgrest["quiz_matches"]
                .select {
                    filter { eq("status", "WAITING") }
                    limit(1)
                }
                .decodeSingleOrNull<QuizMatch>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun updateMatchProgress(matchId: String, isPlayer1: Boolean, progress: Int, score: Int) {
        try {
            val update = buildJsonObject {
                if (isPlayer1) {
                    put("player1_progress", progress)
                    put("player1_score", score)
                } else {
                    put("player2_progress", progress)
                    put("player2_score", score)
                }
            }
            supabaseClient.postgrest["quiz_matches"].update(update) {
                filter { eq("match_id", matchId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun finishMatch(matchId: String) {
        try {
            val update = buildJsonObject { put("status", "FINISHED") }
            supabaseClient.postgrest["quiz_matches"].update(update) {
                filter { eq("match_id", matchId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    
    override fun observeMatch(matchId: String): Flow<QuizMatch> = kotlinx.coroutines.flow.flow {
        try {
            supabaseClient.realtime.connect()
            val channel = supabaseClient.realtime.channel("public:quiz_matches:$matchId")
            val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "quiz_matches"
            }
            channel.subscribe()
            flow.collect { action ->
                try {
                    val jsonStr = action.record.toString()
                    val match = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<QuizMatch>(jsonStr)
                    if (match.matchId == matchId) {
                        emit(match)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}