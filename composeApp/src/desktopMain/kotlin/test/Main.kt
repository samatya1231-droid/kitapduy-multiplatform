package test

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import io.github.jan.supabase.serializer.KotlinXSerializer
import com.semtinazizi.kitapduy.model.Book

fun main() = runBlocking {
    val client = createSupabaseClient(
        supabaseUrl = "https://hojyvyuoafjogefhtrgg.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imhvanl2eXVvYWZqb2dlZmh0cmdnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzEzNTg2NzIsImV4cCI6MjA4NjkzNDY3Mn0.8Ia4a5wwzL_XYbhMW7pOztZfqwoGKoK_Mom1cqv7z2Q"
    ) {
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        })
        install(Postgrest)
    }

    try {
        val books = client.postgrest["books"].select().decodeList<Book>()
        println("SUCCESS: loaded " + books.size + " books")
    } catch (e: Exception) {
        println("CRASH: " + e.message)
        e.printStackTrace()
    }
}
