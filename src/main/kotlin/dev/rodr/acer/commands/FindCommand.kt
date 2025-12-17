package dev.rodr.acer.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.rodr.acer.annotations.InitCommand
import dev.rodr.acer.config.AcerConfig
import dev.rodr.acer.effect.Marker
import me.lucko.fabric.api.permissions.v0.Permissions
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.item.ItemArgument
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.server.permissions.PermissionLevel
import net.minecraft.world.Container
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

object FindCommand : BaseCommand {
    @InitCommand
    override fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        registryAccess: CommandBuildContext,
        environment: Commands.CommandSelection
    ) {
        dispatcher.register(
            Commands.literal("find")
                .requires { source ->
                    source.isPlayer && Permissions.check(
                        source,
                        "acer.find",
                        PermissionLevel.MODERATORS
                    )
                }
                .executes(::execute)
                .then(
                    Commands.argument("item", ItemArgument(registryAccess))
                        .executes(::execute)
                )
        )
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        val world = ctx.source.level
        val player = ctx.source.playerOrException
        val (range) = AcerConfig.data.finder
        val item = try {
            ItemArgument.getItem(ctx, "item").item
        } catch (_: java.lang.IllegalArgumentException) {
            listOf(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                player.getItemInHand(InteractionHand.OFF_HAND)
            ).firstOrNull { !it.isEmpty }?.item
        } ?: throw SimpleCommandExceptionType(Component.literal("No item to find")).create()


        var amount = 0
        findContainers(world, player.blockPosition(), range, item).forEach { position ->
            amount++
            Marker.spawn(world, position)
        }

        val message = Component.empty().apply {
            if (amount == 0) {
                append(Component.literal("No "))
                append(Component.translatable(item.descriptionId))
                append(Component.literal(" found!"))
            } else {
                append(Component.literal("Found $amount "))
                append(Component.translatable(item.descriptionId))
            }
            style = Style.EMPTY.withColor(ChatFormatting.RED)
        }

        player.sendSystemMessage(message, true)

        return amount
    }

    private fun findContainers(world: Level, center: BlockPos, range: Int, item: Item) = sequence<BlockPos> {
        val searchFor = setOf(item)
        for (x in center.x - range..center.x + range) {
            for (y in center.y - range..center.y + range) {
                for (z in center.z - range..center.z + range) {
                    val pos = BlockPos(x, y, z)
                    val block = world.getBlockEntity(pos)
                    if (block is Container && block.hasAnyOf(searchFor)) {
                        yield(pos)
                    }
                }
            }
        }
    }
}