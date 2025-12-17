package dev.rodrick.acer.commands

import com.mojang.brigadier.CommandDispatcher
import dev.rodrick.acer.AcerMod
import dev.rodrick.acer.annotations.InitCommand
import dev.rodrick.acer.config.AcerConfig
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.permissions.PermissionLevel

object AcerCommand : BaseCommand {
    @InitCommand
    override fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        registryAccess: CommandBuildContext,
        environment: Commands.CommandSelection
    ) {
        dispatcher.register(
            Commands.literal("acer")
                .requires { source -> Permissions.check(source, "acer.admin", PermissionLevel.ADMINS) }
                .then(reloadConfig)
                .then(listConfig))

    }

    private val reloadConfig = Commands.literal("reload").executes { context ->
        try {
            AcerConfig.load()
            context.source.sendSuccess({ Component.literal("[Acer] Config reloaded") }, true)
            0
        } catch (e: Exception) {
            AcerMod.logger.warn("Error reloading config: $e")
            context.source.sendFailure(
                Component.literal("[Acer] Error reloading config, check console for more information")
            )
            1
        }
    }

    private val listConfig = Commands.literal("config").executes { context ->
        context.source.sendSuccess(
            { Component.literal(AcerConfig.data.toString()) }, false
        )
        0
    }
}