package com.semtinazizi.kitapduy.ui.viewmodel

import com.semtinazizi.kitapduy.data.model.QuizLeaderboardEntry
import com.semtinazizi.kitapduy.data.model.QuizMatch
import com.semtinazizi.kitapduy.domain.repository.AuthRepository
import com.semtinazizi.kitapduy.domain.repository.QuizMultiplayerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class QuizScreenModelTest {
    private fun auth(uid: String? = "me") = Proxy.newProxyInstance(
        AuthRepository::class.java.classLoader,
        arrayOf(AuthRepository::class.java)
    ) { _, method, _ ->
        if (method.name == "getCurrentUserUid") uid else null
    } as AuthRepository

    @Test
    fun loadLeaderboardTransitionsStateAndPopulatesRemoteEntriesAndResetReturnsStarting() = runTest {
        val remoteEntries = listOf(
            QuizLeaderboardEntry(userId = "u1", userName = "Elif Yılmaz", totalScore = 2400),
            QuizLeaderboardEntry(userId = "u2", userName = "Mehmet Demir", totalScore = 1850)
        )
        val model = QuizScreenModel(
            authRepo = auth(),
            multiplayerRepo = FakeQuizMultiplayerRepo(leaderboard = remoteEntries),
            suppliedScope = backgroundScope
        )

        assertEquals(QuizGameState.STARTING, model.gameState.value)

        model.loadLeaderboard()
        runCurrent()

        assertEquals(QuizGameState.LEADERBOARD, model.gameState.value)
        assertEquals(remoteEntries, model.leaderboard.value)

        model.resetGame()
        assertEquals(QuizGameState.STARTING, model.gameState.value)
    }

    @Test
    fun loadLeaderboardFallsBackToSeedEntriesWhenRemoteFails() = runTest {
        val model = QuizScreenModel(
            authRepo = auth(),
            multiplayerRepo = FakeQuizMultiplayerRepo(shouldFailLeaderboard = true),
            suppliedScope = backgroundScope
        )

        model.loadLeaderboard()
        runCurrent()

        assertEquals(QuizGameState.LEADERBOARD, model.gameState.value)
        assertTrue(model.leaderboard.value.isNotEmpty())
        assertTrue(model.leaderboard.value.any { it.userName == "Elif Yılmaz" })
    }

    @Test
    fun startGameTransitionsOutOfStartingAndResetReturnsStarting() = runTest {
        val model = QuizScreenModel(
            authRepo = auth(),
            multiplayerRepo = FakeQuizMultiplayerRepo(),
            suppliedScope = backgroundScope
        )

        model.startGame()
        assertEquals(QuizGameState.PLAYING, model.gameState.value)

        model.resetGame()
        assertEquals(QuizGameState.STARTING, model.gameState.value)
    }

    @Test
    fun loadLeaderboardTransitionsImmediatelyBeforeRemoteCompletesAndResetCancelsJob() = runTest {
        val repo = FakeQuizMultiplayerRepo(leaderboard = listOf(QuizLeaderboardEntry("u1", "Ada", 900)))
        val model = QuizScreenModel(
            authRepo = auth(),
            multiplayerRepo = repo,
            suppliedScope = backgroundScope
        )

        model.loadLeaderboard()
        assertEquals(QuizGameState.LEADERBOARD, model.gameState.value)

        model.resetGame()
        assertEquals(QuizGameState.STARTING, model.gameState.value)
        runCurrent()
        assertEquals(QuizGameState.STARTING, model.gameState.value)
        assertTrue(model.leaderboard.value.isEmpty())
    }

    @Test
    fun submitAnswerAdvancesQuestionAndResetCancelsPendingJobs() = runTest {
        val model = QuizScreenModel(
            authRepo = auth(),
            multiplayerRepo = FakeQuizMultiplayerRepo(),
            suppliedScope = backgroundScope
        )

        model.startGame(ArenaRound.BLITZ)
        assertEquals(QuizGameState.PLAYING, model.gameState.value)
        assertEquals(0, model.currentQuestionIndex.value)

        // Advance timer by 1 second to confirm timerJob runs on suppliedScope.
        advanceTimeBy(1001)
        assertEquals(9, model.timeLeft.value)

        // Answer and advance past 1500ms to reach the next question.
        val q0 = model.currentQuestion.value!!
        model.submitAnswer(q0.correctIndex)
        assertEquals(true, model.isCorrect.value)
        advanceTimeBy(1501)
        assertEquals(1, model.currentQuestionIndex.value)
        assertNull(model.selectedAnswer.value)

        // Answer question 1, then immediately reset before 1500ms elapses.
        val q1 = model.currentQuestion.value!!
        model.submitAnswer(q1.correctIndex)
        model.resetGame()
        assertEquals(QuizGameState.STARTING, model.gameState.value)
        assertEquals(0, model.score.value)
        assertEquals(3, model.lives.value)

        // Advancing time must NOT re-trigger question advancement or timer countdown.
        advanceTimeBy(20_000)
        assertEquals(QuizGameState.STARTING, model.gameState.value)
        assertEquals(1, model.currentQuestionIndex.value)
        assertEquals(3, model.lives.value)
    }

    @Test
    fun literatureRoundContinuesAfterAWrongAnswerAndFinishesAfterFiveQuestions() = runTest {
        val model = QuizScreenModel(
            authRepo = auth(),
            multiplayerRepo = FakeQuizMultiplayerRepo(),
            suppliedScope = backgroundScope
        )

        model.startGame(ArenaRound.LITERATURE)
        advanceTimeBy(16_000)
        assertEquals(0, model.currentQuestionIndex.value)
        assertNull(model.selectedAnswer.value)
        repeat(5) { index ->
            val question = requireNotNull(model.currentQuestion.value)
            val choice = if (index == 0) (question.correctIndex + 1) % question.options.size else question.correctIndex
            model.submitAnswer(choice)
            advanceTimeBy(1501)
        }
        assertEquals(QuizGameState.SUCCESS, model.gameState.value)
        assertEquals(80, model.score.value)
    }

    @Test
    fun duelHostAndGuestStartWithTheSameQuestionAfterOpponentJoins() = runTest {
        val waiting = QuizMatch(matchId = "match_1", player1Id = "host", player1Name = "Ev sahibi")
        val playing = waiting.copy(player2Id = "guest", player2Name = "Misafir", status = "PLAYING")
        val hostRepo = FakeQuizMultiplayerRepo(createdRoom = waiting, observedRoom = playing)
        val guestRepo = FakeQuizMultiplayerRepo(waitingRoom = waiting, joinedRoom = playing, observedRoom = playing)
        val host = QuizScreenModel(auth("host"), hostRepo, backgroundScope)
        val guest = QuizScreenModel(auth("guest"), guestRepo, backgroundScope)

        host.findMultiplayerMatch()
        guest.findMultiplayerMatch()
        runCurrent()

        assertEquals(QuizGameState.PLAYING, host.gameState.value)
        assertEquals(QuizGameState.PLAYING, guest.gameState.value)
        assertEquals(host.currentQuestion.value?.id, guest.currentQuestion.value?.id)
        assertEquals("Misafir", host.opponent.value?.name)
        assertEquals("Ev sahibi", guest.opponent.value?.name)
    }
}

private class FakeQuizMultiplayerRepo(
    var leaderboard: List<QuizLeaderboardEntry> = emptyList(),
    var shouldFailLeaderboard: Boolean = false,
    private val createdRoom: QuizMatch? = null,
    private val waitingRoom: QuizMatch? = null,
    private val joinedRoom: QuizMatch? = null,
    private val observedRoom: QuizMatch? = null
) : QuizMultiplayerRepository {
    override suspend fun getLeaderboard(): List<QuizLeaderboardEntry> {
        if (shouldFailLeaderboard) error("Leaderboard unavailable")
        return leaderboard
    }

    override suspend fun updateLeaderboard(userId: String, userName: String, scoreToAdd: Long) {}
    override suspend fun createMatch(userId: String, userName: String, roomCode: String?): QuizMatch? = createdRoom
    override suspend fun joinMatch(matchId: String, userId: String, userName: String): QuizMatch? = joinedRoom
    override suspend fun findWaitingMatch(excludingUserId: String): QuizMatch? = waitingRoom
    override suspend fun updateMatchProgress(matchId: String, isPlayer1: Boolean, progress: Int, score: Int) {}
    override suspend fun cancelWaitingMatch(matchId: String, hostId: String) {}
    override fun observeMatch(matchId: String) = observedRoom?.let { flowOf(it) } ?: emptyFlow()
}
