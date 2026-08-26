import java.net.URL
import java.util.regex.Pattern

fun parseEpisodes(xmlContent: String) {
    val itemRegex = "<item>(.*?)</item>".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val titleRegex = "<title>(.*?)</title>".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val enclosureRegex = "<enclosure[^>]+url\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>".toRegex(RegexOption.IGNORE_CASE)
    
    val items = itemRegex.findAll(xmlContent)
    var count = 0
    for (itemMatch in items) {
        val itemContent = itemMatch.groupValues[1]
        val title = titleRegex.find(itemContent)?.groupValues?.get(1)?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim()
        val audioUrl = enclosureRegex.find(itemContent)?.groupValues?.get(1)?.trim()
        if (title == null) println("Missing title")
        if (audioUrl == null) println("Missing audioUrl")
        if (title != null && audioUrl != null) count++
    }
    println("Total matched: $count")
}

val urls = listOf(
    "https://anchor.fm/s/44a38a74/podcast/rss",
    "https://feeds.simplecast.com/qm_9xx0g",
    "https://feeds.megaphone.fm/fularsizentellik",
    "https://feeds.podbee.com/111hz"
)

for (u in urls) {
    println("Testing $u")
    try {
        val content = URL(u).readText()
        println("Content size: ${content.length}")
        parseEpisodes(content)
    } catch (e: Exception) {
        println("Error: ${e.message}")
    }
}
