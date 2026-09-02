package scratch

import kotlinx.coroutines.runBlocking
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

fun main() = runBlocking {
    try {
        val client = createSupabaseClient(
            supabaseUrl = "https://hojyvyuoafjogefhtrgg.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhvanl2eXVvYWZqb2dlZmh0cmdnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzEzNTg2NzIsImV4cCI6MjA4NjkzNDY3Mn0.8Ia4a5wwzL_XYbhMW7pOztZfqwoGKoK_Mom1cqv7z2Q"
        ) {
            install(Postgrest)
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }

        println("Fetching books...")
        val books = client.postgrest["books"].select().decodeList<JsonObject>()
        println("Books count: ${books.size}")
        
    } catch (e: Exception) {
        println("Exception: ${e.message}")
        e.printStackTrace()
    }
}
