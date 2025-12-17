package dev.rodr.acer.callbacks

import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory

fun interface NotWhitelistedCallback {
    fun onDisconnected(player: String?, ip: String)

    companion object {
        val EVENT: Event<NotWhitelistedCallback> =
            EventFactory.createArrayBacked(NotWhitelistedCallback::class.java) { listeners ->
                NotWhitelistedCallback { player, ip ->
                    listeners.forEach {
                        it.onDisconnected(player, ip)
                    }
                }
            }
    }
}