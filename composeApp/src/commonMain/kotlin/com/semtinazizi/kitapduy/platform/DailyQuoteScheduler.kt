package com.semtinazizi.kitapduy.platform

/**
 * Platform specific scheduler for daily quotes from Firebase.
 */
expect object DailyQuoteScheduler {
    fun scheduleDaily(context: Any? = null)
}
