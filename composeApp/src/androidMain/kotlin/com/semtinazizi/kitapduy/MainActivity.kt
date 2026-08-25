package com.semtinazizi.kitapduy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.semtinazizi.kitapduy.data.SupabaseManager
import com.semtinazizi.kitapduy.model.PoetryItem
import com.semtinazizi.kitapduy.platform.DailyQuoteScheduler
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    companion object {
        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()
        
        DailyQuoteScheduler.scheduleDaily(this)
        
        // MIGRATION SCRIPT: Firebase'den Supabase'e verileri taşır.
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("MIGRATION", "Supabase başlatılıyor (Manuel)...")
                com.semtinazizi.kitapduy.data.SupabaseManager.initialize(
                    supabaseUrl = BuildConfig.SUPABASE_URL,
                    supabaseKey = BuildConfig.SUPABASE_KEY
                )
                
                val supabase = com.semtinazizi.kitapduy.data.SupabaseManager.client
                
                // Podcastler Migrasyonu
                Log.d("MIGRATION", "Supabase podcast kontrolü yapılıyor...")
                val response = supabase.postgrest["podcasts"].select().data
                
                if (response == "[]") {
                    Log.d("MIGRATION", "Supabase 'podcasts' tablosu boş. Firebase'den çekiliyor...")
                    val podSnapshot = Firebase.firestore.collection("podcasts").get().await()
                    Log.d("MIGRATION", "Firebase'de ${podSnapshot.size()} podcast bulundu.")
                    
                    for (doc in podSnapshot.documents) {
                        try {
                            val name = doc.getString("name") ?: doc.getString("title") ?: "İsimsiz"
                            val pod = com.semtinazizi.kitapduy.model.Podcast(
                                id = doc.id,
                                name = name,
                                author = doc.getString("author") ?: "",
                                rssUrl = doc.getString("rss_url") ?: doc.getString("res_url") ?: "",
                                imageUrl = doc.getString("image_url") ?: doc.getString("coverUrl") ?: "",
                                category = doc.getString("category") ?: "Genel"
                            )
                            supabase.postgrest["podcasts"].insert(pod)
                            Log.d("MIGRATION", "Taşındı: $name")
                        } catch (e: Exception) { Log.e("MIGRATION", "Tekil podcast hatası: ${e.message}") }
                    }
                    Log.d("MIGRATION", "Podcast migrasyonu bitti.")
                } else {
                    Log.d("MIGRATION", "Supabase'de zaten podcast verisi mevcut.")
                }
            } catch (e: Exception) {
                Log.e("MIGRATION", "Geri bildirim: ${e.message}")
            }
        }
        
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        if (instance == this) instance = null
        super.onDestroy()
    }
}
