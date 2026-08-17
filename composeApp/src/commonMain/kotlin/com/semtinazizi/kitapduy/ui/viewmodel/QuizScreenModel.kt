package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.model.QuizQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class QuizGameState {
    STARTING, PLAYING, GAME_OVER, SUCCESS
}

class QuizScreenModel : ScreenModel {

    // Dummy Questions for now, eventually fetched from Supabase
    private val allQuestions = listOf(
        QuizQuestion("1", "\"Kürk Mantolu Madonna\" kitabının yazarı kimdir?", listOf("Sabahattin Ali", "Orhan Pamuk", "Halide Edip Adıvar", "Yakup Kadri Karaosmanoğlu"), 0, "Alman bir ressama aşık olan Raif Efendi..."),
        QuizQuestion("2", "\"Suç ve Ceza\" romanının başkarakteri kimdir?", listOf("Mişkin", "Raskolnikov", "Karamazov", "İvan İlyiç"), 1, "Baltalı bir tefeci cinayeti..."),
        QuizQuestion("3", "İlk Türkçe roman olan \"Taaşşuk-ı Talat ve Fitnat\" kimin eseridir?", listOf("Namık Kemal", "Şemsettin Sami", "Ahmet Mithat Efendi", "Recaizade Mahmut Ekrem"), 1, "Sami kelimesini içerir..."),
        QuizQuestion("4", "\"Tutunamayanlar\" eseriyle Oğuz Atay hangi akımın öncüsü sayılır?", listOf("Romantizm", "Klasisizm", "Postmodernizm", "Realizm"), 2, "Modern sonrası...")
    ).shuffled()

    private val _gameState = MutableStateFlow(QuizGameState.STARTING)
    val gameState: StateFlow<QuizGameState> = _gameState.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow<QuizQuestion?>(null)
    val currentQuestion: StateFlow<QuizQuestion?> = _currentQuestion.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _lives = MutableStateFlow(3)
    val lives: StateFlow<Int> = _lives.asStateFlow()

    private val _timeLeft = MutableStateFlow(15) // Her soru 15 saniye
    val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()
    
    private val _selectedAnswer = MutableStateFlow<Int?>(null)
    val selectedAnswer: StateFlow<Int?> = _selectedAnswer.asStateFlow()
    
    private val _isCorrect = MutableStateFlow<Boolean?>(null)
    val isCorrect: StateFlow<Boolean?> = _isCorrect.asStateFlow()

    private var timerJob: Job? = null

    fun startGame() {
        _score.value = 0
        _lives.value = 3
        _currentQuestionIndex.value = 0
        _gameState.value = QuizGameState.PLAYING
        loadNextQuestion()
    }

    private fun loadNextQuestion() {
        if (_lives.value <= 0) {
            _gameState.value = QuizGameState.GAME_OVER
            return
        }

        if (_currentQuestionIndex.value >= allQuestions.size) {
            _gameState.value = QuizGameState.SUCCESS
            return
        }

        _selectedAnswer.value = null
        _isCorrect.value = null
        _currentQuestion.value = allQuestions[_currentQuestionIndex.value]
        _timeLeft.value = 15
        
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = screenModelScope.launch {
            while (isActive && _timeLeft.value > 0) {
                delay(1000L)
                _timeLeft.value -= 1
            }
            if (_timeLeft.value <= 0) {
                // Zaman doldu, yanlış kabul et
                handleAnswer(-1)
            }
        }
    }

    fun submitAnswer(selectedIndex: Int) {
        if (_selectedAnswer.value != null || _gameState.value != QuizGameState.PLAYING) return
        handleAnswer(selectedIndex)
    }

    private fun handleAnswer(selectedIndex: Int) {
        timerJob?.cancel()
        _selectedAnswer.value = selectedIndex
        
        val question = _currentQuestion.value ?: return
        
        if (selectedIndex == question.correctIndex) {
            _isCorrect.value = true
            _score.value += 10 + _timeLeft.value // Hızlı cevap ekstra puan
        } else {
            _isCorrect.value = false
            _lives.value -= 1
        }
        
        // Biraz bekle ve sonraki soruya geç
        screenModelScope.launch {
            delay(1500)
            if (_lives.value > 0) {
                _currentQuestionIndex.value += 1
                loadNextQuestion()
            } else {
                _gameState.value = QuizGameState.GAME_OVER
            }
        }
    }
    
    override fun onDispose() {
        super.onDispose()
        timerJob?.cancel()
    }
}
