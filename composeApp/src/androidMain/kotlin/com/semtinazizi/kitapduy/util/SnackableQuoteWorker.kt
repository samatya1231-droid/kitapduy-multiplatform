package com.semtinazizi.kitapduy.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.semtinazizi.kitapduy.MainActivity
import com.semtinazizi.kitapduy.R
import kotlinx.coroutines.tasks.await

class SnackableQuoteWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result {
        return try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection("daily_quotes_v2").get().await()
            
            if (!snapshot.isEmpty) {
                val quotes = snapshot.documents.filter { doc ->
                    val text = doc.getString("text") ?: ""
                    text.isNotBlank()
                }
                
                if (quotes.isNotEmpty()) {
                    val randomQuote = quotes.random()
                    val text = randomQuote.getString("text") ?: ""
                    val author = randomQuote.getString("author") ?: "KitapDuy"
                    
                    showNotification(text, author)
                }
            }
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.failure()
        }
    }

    private fun showNotification(text: String, author: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_quote_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Günün İlhamı",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Günün alıntılarını ve hap bilgilerini gösterir."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Make sure R.drawable.ic_launcher_foreground exists, or fallback to something else if needed.
        // It should exist in standard KMP template or Android template.
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using android system icon as fallback
            .setContentTitle("✨ Günün İlhamı")
            .setContentText("“$text” — $author")
            .setStyle(NotificationCompat.BigTextStyle().bigText("“$text”\n\n— $author"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}
