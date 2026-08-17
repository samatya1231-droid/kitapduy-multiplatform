package com.semtinazizi.kitapduy.ui.viewmodel

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.semtinazizi.kitapduy.platform.AudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusScreenModel(
    private val audioPlayer: AudioPlayer
) : ScreenModel {

    // Varsayılan süre: 25 Dk = 25 * 60 * 1000L
    private val _totalTime = MutableStateFlow(25 * 60 * 1000L)
    val totalTime: StateFlow<Long> = _totalTime.asStateFlow()

    private val _timeLeft = MutableStateFlow(25 * 60 * 1000L)
    val timeLeft: StateFlow<Long> = _timeLeft.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _currentRadio = MutableStateFlow<String?>(null)
    val currentRadio: StateFlow<String?> = _currentRadio.asStateFlow()

    private var timerJob: Job? = null

    fun setTime(minutes: Int) {
        val newTime = minutes * 60 * 1000L
        _totalTime.value = newTime
        _timeLeft.value = newTime
        stopTimer()
    }

    fun startPauseTimer() {
        if (_isRunning.value) {
            stopTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_timeLeft.value <= 0) return
        _isRunning.value = true
        timerJob = screenModelScope.launch {
            while (isActive && _timeLeft.value > 0) {
                delay(1000L)
                _timeLeft.value -= 1000L
            }
            if (_timeLeft.value <= 0) {
                _isRunning.value = false
            }
        }
    }

    private fun stopTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        stopTimer()
        _timeLeft.value = _totalTime.value
    }

    fun playRadio(url: String, title: String) {
        if (_currentRadio.value == url) {
            audioPlayer.pause()
            _currentRadio.value = null
        } else {
            audioPlayer.play(url)
            _currentRadio.value = url
        }
    }
    
    override fun onDispose() {
        super.onDispose()
        stopTimer()
        if (_currentRadio.value != null) {
            audioPlayer.pause()
        }
    }
}
