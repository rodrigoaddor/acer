package dev.rodrick.acer.events

import dev.rodrick.acer.AcerMod
import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.config.AcerConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

object PlayerNotifier {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    private const val ENDPOINT = "https://joinjoaomgcd.appspot.com/_ah/api/messaging/v1/sendPush"

    @Init
    fun init() {
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            if (AcerConfig.data.notifier.onJoin) {
                sendNotification("Player joined", "${handler.player.name.string} joined the server")
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            if (AcerConfig.data.notifier.onLeave) {
                sendNotification("Player left", "${handler.player.name.string} left the server")
            }
        }
    }

    private fun sendNotification(title: String, text: String?) = runBlocking {
        val (_, _, apiKey, devices) = AcerConfig.data.notifier

        val body = NotificationOptions(apiKey, devices.joinToString(","), title, text)

        AcerMod.logger.info("Sending notification, $body")

        launch {
            val response = client.post(ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }

            val (success, error) = response.body<JoinResponse>()
            if (!success) {
                AcerMod.logger.warn("Error sending notification: ${error ?: "Unknown error"}")
            }
        }
    }

    @Serializable
    data class NotificationOptions(
        val apikey: String,
        val deviceNames: String,
        val title: String,
        val text: String?,
    )

    @Serializable
    data class JoinResponse(
        val success: Boolean,
        val errorMessage: String? = null
    )
}