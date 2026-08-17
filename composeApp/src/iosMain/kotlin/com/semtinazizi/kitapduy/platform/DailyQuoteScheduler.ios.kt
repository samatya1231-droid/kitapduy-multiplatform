package com.semtinazizi.kitapduy.platform

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import kotlinx.serialization.json.*
import platform.Foundation.*
import platform.UserNotifications.*

actual object DailyQuoteScheduler {

    // Simple Ktor client for iOS to fetch Firestore REST API
    private val client = HttpClient()

    actual fun scheduleDaily(context: Any?) {
        // iOS requests permission and schedules ahead of time
        val center = UNUserNotificationCenter.currentNotificationCenter()
        
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, error ->
            if (granted) {
                // Fetch and schedule in background
                @OptIn(DelicateCoroutinesApi::class)
                GlobalScope.launch(Dispatchers.Default) {
                    try {
                        val quotes = fetchQuotesFromFirestore()
                        if (quotes.isNotEmpty()) {
                            scheduleNotificationsForNextDays(quotes, center)
                        }
                    } catch (e: Exception) {
                        println("Failed to fetch/schedule iOS quotes: ${e.message}")
                    }
                }
            }
        }
    }

    private suspend fun fetchQuotesFromFirestore(): List<Pair<String, String>> {
        val url = "https://firestore.googleapis.com/v1/projects/kitapduy/databases/(default)/documents/daily_quotes_v2"
        val response = client.get(url)
        val jsonString = response.bodyAsText()
        
        val quotes = mutableListOf<Pair<String, String>>()
        try {
            val json = Json { ignoreUnknownKeys = true }
            val root = json.parseToJsonElement(jsonString).jsonObject
            val documents = root["documents"]?.jsonArray
            
            documents?.forEach { doc ->
                val fields = doc.jsonObject["fields"]?.jsonObject
                val text = fields?.get("text")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: ""
                val author = fields?.get("author")?.jsonObject?.get("stringValue")?.jsonPrimitive?.content ?: "KitapDuy"
                
                if (text.isNotBlank()) {
                    quotes.add(text to author)
                }
            }
        } catch (e: Exception) {
            println("JSON parsing error: ${e.message}")
        }
        
        return quotes.shuffled()
    }

    private fun scheduleNotificationsForNextDays(
        quotes: List<Pair<String, String>>, 
        center: UNUserNotificationCenter
    ) {
        // Remove existing scheduled ones to avoid duplicates
        center.removeAllPendingNotificationRequests()

        val now = Clock.System.now()
        val systemTZ = TimeZone.currentSystemDefault()
        
        // Schedule for the next 7 days at 10:00 AM
        for (i in 1..minOf(7, quotes.size)) {
            val quote = quotes[i - 1]
            val content = UNMutableNotificationContent().apply {
                setTitle("✨ Günün İlhamı")
                setBody("“${quote.first}”\n\n— ${quote.second}")
                setSound(UNNotificationSound.defaultSound)
            }

            // Target time: tomorrow + i days at 10:00 AM
            val targetDateTime = now.plus(i, DateTimeUnit.DAY, systemTZ)
                .toLocalDateTime(systemTZ)
                
            val dateComponents = NSDateComponents().apply {
                setHour(10)
                setMinute(0)
                setDay(targetDateTime.dayOfMonth.toLong())
                setMonth(targetDateTime.monthNumber.toLong())
                setYear(targetDateTime.year.toLong())
            }

            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents, 
                repeats = false
            )

            val request = UNNotificationRequest.requestWithIdentifier(
                "daily_quote_$i", 
                content, 
                trigger
            )

            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Error scheduling notification: ${error.localizedDescription}")
                }
            }
        }
    }
}
