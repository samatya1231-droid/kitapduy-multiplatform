import os

path = "C:/Users/Rahmi/AndroidStudioProjects/kitapduy-multiplatform/composeApp/src/commonMain/kotlin/com/semtinazizi/kitapduy/data/repository/SupabaseQuizMultiplayerRepository.kt"
with open(path, "r", encoding="utf-8") as f:
    code = f.read()

new_observe = """
    override fun observeMatch(matchId: String): Flow<QuizMatch> = kotlinx.coroutines.flow.flow {
        try {
            supabaseClient.realtime.connect()
            val channel = supabaseClient.realtime.channel("public:quiz_matches:$matchId")
            val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "quiz_matches"
            }
            channel.subscribe()
            flow.collect { action ->
                try {
                    val jsonStr = action.record.toString()
                    val match = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<QuizMatch>(jsonStr)
                    if (match.matchId == matchId) {
                        emit(match)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
"""

if "override fun observeMatch" in code:
    code = code.split("override fun observeMatch")[0] + new_observe + "}"

with open(path, "w", encoding="utf-8") as f:
    f.write(code)

print("SUCCESS")
