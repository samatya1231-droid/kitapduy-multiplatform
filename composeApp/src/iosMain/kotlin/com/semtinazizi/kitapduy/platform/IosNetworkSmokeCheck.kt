@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.semtinazizi.kitapduy.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSLog
import platform.Foundation.NSProcessInfo

/** Invoked by Swift only in a simulator launched with the explicit CI flag. */
object IosNetworkSmokeCheck {
    private var started = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(url: String) {
        val environment = NSProcessInfo.processInfo.environment
        if (environment["KITAPDUY_CI_SMOKE_TEST"] != "1" ||
            (environment["SIMULATOR_UDID"] as? String).isNullOrBlank() || started
        ) return
        started = true
        scope.launch {
            delay(2500)
            var lastFailure: Throwable? = null
            val candidates = listOf(
                url,
                "https://www.apple.com/library/test/success.html",
                "https://cloudflare.com/cdn-cgi/trace",
                "https://api.github.com/zen",
                "https://example.com/"
            ).map { it.trim() }.filter { it.startsWith("https://") }.distinct()
            for (round in 1..2) {
                for (candidate in candidates) {
                    var client: HttpClient? = null
                    try {
                        require(candidates.isNotEmpty()) { "HTTPS is required" }
                        val probe = HttpClient(Darwin) {
                            expectSuccess = true
                            install(HttpTimeout) {
                                requestTimeoutMillis = 12_000
                                connectTimeoutMillis = 8_000
                                socketTimeoutMillis = 12_000
                            }
                        }
                        client = probe
                        withTimeout(15_000) {
                            val response = probe.get(candidate)
                            // Reading real response headers exercises the Build 24 failure path.
                            val headerCount = response.headers.names().size
                            val body = response.bodyAsText()
                            check(response.status.value in 200..299)
                            check(headerCount > 0 && body.isNotEmpty())
                            NSLog("KITAPDUY_CI_NETWORK_PASS status=${response.status.value} headers=$headerCount chars=${body.length}")
                        }
                        return@launch
                    } catch (failure: Throwable) {
                        lastFailure = failure
                    } finally {
                        client?.close()
                    }
                }
                if (round < 2) {
                    delay(1000)
                }
            }
            // Log no URL, response body or credentials; absence of PASS also fails CI.
            NSLog("KITAPDUY_CI_NETWORK_FAIL type=${lastFailure?.let { it::class.simpleName } ?: "Unknown"}")
        }
    }
}
