package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuoteQuestion(
    val quote: String,
    val correctAnswer: String,
    val options: List<String>
)

enum class GuessAuthorState {
    LOADING, PLAYING, GAME_OVER
}

class GuessAuthorScreenModel : ScreenModel {

    // Şimdilik Supabase bağlantısı olmadığı için dummy sorular
    private val allQuestions = listOf(
        QuoteQuestion("İçimizde şeytan yok... İçimizde acız var, tembellik var.", "Sabahattin Ali", listOf("Sait Faik Abasıyanık", "Sabahattin Ali", "Ömer Seyfettin", "Ahmet Hamdi Tanpınar")),
        QuoteQuestion("Bütün mutlu aileler birbirine benzer; her mutsuz ailenin mutsuzluğu ise kendine göredir.", "Lev Tolstoy", listOf("Fyodor Dostoyevski", "Lev Tolstoy", "Anton Çehov", "Maksim Gorki")),
        QuoteQuestion("İnsan dünyada en çok kendi içine düşer.", "Nietzsche", listOf("Kant", "Nietzsche", "Schopenhauer", "Spinoza"))
    ).shuffled()

    private val _gameState = MutableStateFlow(GuessAuthorState.LOADING)
    val gameState: StateFlow<GuessAuthorState> = _gameState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow<QuoteQuestion?>(null)
    val currentQuestion: StateFlow<QuoteQuestion?> = _currentQuestion.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives: StateFlow<Int> = _lives.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<String?>(null)
    val selectedAnswer: StateFlow<String?> = _selectedAnswer.asStateFlow()

    private val _showResultColor = MutableStateFlow(false)
    val showResultColor: StateFlow<Boolean> = _showResultColor.asStateFlow()

    init {
        startGame()
    }

    fun startGame() {
        _score.value = 0
        _lives.value = 3
        _currentQuestionIndex.value = 0
        _gameState.value = GuessAuthorState.PLAYING
        loadQuestion()
    }

    private fun loadQuestion() {
        if (_lives.value <= 0) {
            _gameState.value = GuessAuthorState.GAME_OVER
            return
        }

        val index = _currentQuestionIndex.value % allQuestions.size
        _currentQuestion.value = allQuestions[index]
        _selectedAnswer.value = null
        _showResultColor.value = false
    }

    fun submitAnswer(answer: String) {
        if (_selectedAnswer.value != null || _gameState.value != GuessAuthorState.PLAYING) return
        
        _selectedAnswer.value = answer
        _showResultColor.value = true

        val isCorrect = answer == _currentQuestion.value?.correctAnswer

        if (isCorrect) {
            _score.value += 10
        } else {
            _lives.value -= 1
        }

        screenModelScope.launch {
            delay(2000)
            if (_lives.value > 0) {
                _currentQuestionIndex.value += 1
                loadQuestion()
            } else {
                _gameState.value = GuessAuthorState.GAME_OVER
            }
        }
    }

    fun useExtraLife() {
        _lives.value = 1
        _currentQuestionIndex.value += 1
        _gameState.value = GuessAuthorState.PLAYING
        loadQuestion()
    }
}
