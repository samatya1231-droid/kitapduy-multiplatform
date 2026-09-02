package scratch

import io.ktor.client.engine.darwin.*

fun test() {
    val e: Throwable = DarwinHttpRequestException(platform.Foundation.NSError())
}
