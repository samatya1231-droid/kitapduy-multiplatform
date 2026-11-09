package com.semtinazizi.kitapduy.platform

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridge for driving UI navigation during iOS Simulator CI smoke simulation.
 * In non-CI and physical device runs, this remains completely inert.
 */
object IosSmokeSimulationBridge {
    private val _isMainScreenMounted = MutableStateFlow(false)
    val isMainScreenMounted = _isMainScreenMounted.asStateFlow()

    private val _targetTab = MutableStateFlow<Tab?>(null)
    val targetTab = _targetTab.asStateFlow()

    private val _targetScreen = MutableStateFlow<Screen?>(null)
    val targetScreen = _targetScreen.asStateFlow()

    private val _popTrigger = MutableStateFlow(0)
    val popTrigger = _popTrigger.asStateFlow()

    fun setMainScreenMounted(mounted: Boolean) {
        _isMainScreenMounted.value = mounted
    }

    fun switchTab(tab: Tab) {
        _targetTab.value = tab
    }

    fun pushScreen(screen: Screen) {
        _targetScreen.value = screen
    }

    fun clearTargetScreen() {
        _targetScreen.value = null
    }

    fun pop() {
        _popTrigger.value += 1
    }

    fun reset() {
        _isMainScreenMounted.value = false
        _targetTab.value = null
        _targetScreen.value = null
        _popTrigger.value = 0
    }
}
