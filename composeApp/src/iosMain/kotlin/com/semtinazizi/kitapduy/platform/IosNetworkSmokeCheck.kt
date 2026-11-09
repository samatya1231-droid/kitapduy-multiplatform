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
            var client: HttpClient? = null
            try {
                require(url.startsWith("https://")) { "HTTPS is required" }
                val probe = HttpClient(Darwin) {
                    expectSuccess = true
                    install(HttpTimeout) {
                        requestTimeoutMillis = 20_000
                        connectTimeoutMillis = 15_000
                        socketTimeoutMillis = 20_000
                    }
                }
                client = probe
                withTimeout(25_000) {
                    val response = probe.get(url)
                    // Reading real response headers exercises the Build 24 failure path.
                    val headerCount = response.headers.names().size
                    val body = response.bodyAsText()
                    check(response.status.value in 200..299)
                    check(headerCount > 0 && body.isNotEmpty())
                    NSLog("KITAPDUY_CI_NETWORK_PASS status=${response.status.value} headers=$headerCount chars=${body.length}")
                }
            } catch (failure: Throwable) {
                // Log no URL, response body or credentials; absence of PASS also fails CI.
                NSLog("KITAPDUY_CI_NETWORK_FAIL type=${failure::class.simpleName}")
            } finally {
                client?.close()
            }
        }
    }
}
