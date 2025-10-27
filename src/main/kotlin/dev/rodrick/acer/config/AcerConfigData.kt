package dev.rodrick.acer.config

import dev.rodrick.acer.hooks.PlayerHooks
import kotlinx.serialization.Serializable

@Serializable
data class AcerConfigData(
    val replantSaplings: ReplantSaplings = ReplantSaplings(),
    val webhooks: Webhooks = Webhooks(),
    val finder: Finder = Finder(),
    val scythes: Boolean = true,
    val disableEndPortal: Boolean = false
) {
    @Serializable
    data class ReplantSaplings(
        val enabled: Boolean = false,
        val chance: Double = 1.0,
    )

    @Serializable
    data class Webhooks(
        val endpoint: String = "",
        val headers: Map<String, String>? = mapOf(),

        val onJoin: PlayerHooks.Options? = PlayerHooks.Options(
            message = $$"Player ${player} joined the game."
        ),
        val onLeave: PlayerHooks.Options? = PlayerHooks.Options(
            message = $$"Player ${player} left the game.",
        ),
        val onNotWhitelisted: PlayerHooks.Options? = PlayerHooks.Options(
            message = $$"Player ${player} (${ip}) tried to join the game, but they are not white-listed."
        )
    )

    @Serializable
    data class Finder(
        val range: Int = 10,
        val duration: Float = 10f
    )
}
