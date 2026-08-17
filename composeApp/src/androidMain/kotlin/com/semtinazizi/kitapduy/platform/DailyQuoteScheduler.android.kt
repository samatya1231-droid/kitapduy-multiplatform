package com.semtinazizi.kitapduy.platform

import android.content.Context
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import com.semtinazizi.kitapduy.util.SnackableQuoteWorker
import java.util.concurrent.TimeUnit

actual object DailyQuoteScheduler {
    actual fun scheduleDaily(context: Any?) {
        val appContext = context as? Context ?: return
        
        // Günde bir kez çalışacak şekilde zamanla
        val quoteWorkRequest = PeriodicWorkRequestBuilder<SnackableQuoteWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(2, TimeUnit.HOURS) // Uygulama açıldıktan bir süre sonra veya ertesi gün aynı saatte
            .build()

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            "DailyQuoteWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            quoteWorkRequest
        )
    }
}
