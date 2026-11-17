package com.semtinazizi.kitapduy.data.repository

import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.data.model.QuizMatch
import com.semtinazizi.kitapduy.domain.repository.QuizMultiplayerRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

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
        } catch (e: Throwable) {
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
        } catch (e: Throwable) {
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
        } catch (e: CancellationException) {
            throw e
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
                    filter {
                        eq("match_id", matchId)
                        eq("status", "WAITING")
                        neq("player1_id", userId)
                        exact("player2_id", null)
                        gte("created_at", (Clock.System.now() - 1.minutes).toString())
                    }
                    select()
                }
                .decodeSingleOrNull<QuizMatch>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun findWaitingMatch(excludingUserId: String): QuizMatch? {
        return try {
            supabaseClient.postgrest["quiz_matches"]
                .select {
                    filter {
                        eq("status", "WAITING")
                        neq("player1_id", excludingUserId)
                        exact("player2_id", null)
                        gte("created_at", (Clock.System.now() - 1.minutes).toString())
                    }
                    order("created_at", Order.ASCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<QuizMatch>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
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
                filter {
                    eq("match_id", matchId)
                    eq("status", "PLAYING")
                    lt(if (isPlayer1) "player1_progress" else "player2_progress", progress)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override suspend fun cancelWaitingMatch(matchId: String, hostId: String) {
        try {
            val update = buildJsonObject { put("status", "FINISHED") }
            supabaseClient.postgrest["quiz_matches"].update(update) {
                filter {
                    eq("match_id", matchId)
                    eq("player1_id", hostId)
                    eq("status", "WAITING")
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    
    override fun observeMatch(matchId: String): Flow<QuizMatch> = flow {
        while (true) {
            val match = supabaseClient.postgrest["quiz_matches"]
                .select { filter { eq("match_id", matchId) } }
                .decodeSingleOrNull<QuizMatch>() ?: break
            emit(match)
            if (match.status == "FINISHED") break
            delay(2_000)
        }
    }.distinctUntilChanged()
}
