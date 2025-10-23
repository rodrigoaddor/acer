package dev.rodrick.acer.hooks

import dev.rodrick.acer.AcerMod
import dev.rodrick.acer.config.AcerConfig
import dev.rodrick.acer.config.AcerConfigData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class Webhooks {
    private val client = HttpClient(CIO)

    protected val config: AcerConfigData.Webhooks
        get() = AcerConfig.data.webhooks

    protected fun send(message: String) {
        if (config.endpoint.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            val response = client.post(config.endpoint) {
                headers {
                    config.headers?.forEach { (key, value) ->
                        append(key, value)
                    }
                }

                setBody(message)
            }

            if (response.status.value.let { it !in 200..299 }) {
                AcerMod.logger.error("Webhook request failed with status ${response.status.value}")
            }
        }
    }

    private val placeholderRegex = Regex("""(?<!\\)\$\{(\w+?)}""")

    protected fun replacePlaceholders(message: String, placeholders: Map<String, String>): String =
        placeholderRegex.replace(message) {
            val key = it.groupValues[1]
            placeholders[key] ?: it.value
        }
}