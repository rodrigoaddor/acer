package dev.rodrick.acer.commands

import com.mojang.brigadier.CommandDispatcher
import dev.rodrick.acer.AcerMod
import dev.rodrick.acer.annotations.InitCommand
import dev.rodrick.acer.config.AcerConfig
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object AcerCommand : BaseCommand {
    @InitCommand
    override fun register(
        dispatcher: CommandDispatcher<ServerCommandSource>,
        registryAccess: CommandRegistryAccess,
        environment: CommandManager.RegistrationEnvironment
    ) {
        dispatcher.register(
            literal("acer")
                .requires { source -> Permissions.check(source, "acer.admin", 3) }
                .then(reloadConfig)
                .then(listConfig)
        )
    }

    private val reloadConfig = literal("reload").executes { context ->
        try {
            AcerConfig.load()
            context.source.sendFeedback({ Text.literal("[Acer] Config reloaded") }, true)
            0
        } catch (e: Exception) {
            AcerMod.logger.warn("Error reloading config: $e")
            context.source.sendFeedback(
                {Text.literal("[Acer] Error reloading config, check console for more information")}, false
            )
            1
        }
    }

    private val listConfig = literal("config").executes { context ->
        context.source.sendFeedback(
            {Text.literal(AcerConfig.data.toString())},
            false
        )
        0
    }
}