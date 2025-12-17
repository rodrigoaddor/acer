package dev.rodr.acer

import dev.rodr.acer.config.AcerConfig
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("UNUSED")
object AcerMod : DedicatedServerModInitializer {
    const val MOD_NAME = "Acer"
    const val MOD_ID = "acer"

    val logger: Logger = LogManager.getLogger(MOD_NAME)

    override fun onInitializeServer() {
        AcerConfig.load()

        Initializer.init()
        CommandRegistrationCallback.EVENT.register(Initializer::initCommands)
    }
}