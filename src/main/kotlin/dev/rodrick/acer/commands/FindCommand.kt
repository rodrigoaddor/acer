package dev.rodrick.acer.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.rodrick.acer.annotations.InitCommand
import dev.rodrick.acer.config.AcerConfig
import dev.rodrick.acer.effect.Marker
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.command.argument.ItemStackArgumentType
import net.minecraft.inventory.Inventory
import net.minecraft.item.Item
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object FindCommand : BaseCommand {
    @InitCommand
    override fun register(
        dispatcher: CommandDispatcher<ServerCommandSource>,
        registryAccess: CommandRegistryAccess,
        environment: CommandManager.RegistrationEnvironment
    ) {
        dispatcher.register(
            literal("find")
                .requires { source -> source.hasPermissionLevel(4) && source.isExecutedByPlayer }
                .executes(::execute)
                .then(
                    argument("item", ItemStackArgumentType.itemStack(registryAccess))
                        .executes(::execute)
                )
        )
    }

    private fun execute(ctx: CommandContext<ServerCommandSource>): Int {
        val world = ctx.source.world
        val player = ctx.source.playerOrThrow
        val (range) = AcerConfig.data.finder
        val item = try {
            ItemStackArgumentType.getItemStackArgument(ctx, "item").item
        } catch (_: java.lang.IllegalArgumentException) {
            listOf(
                player.getStackInHand(Hand.MAIN_HAND),
                player.getStackInHand(Hand.OFF_HAND)
            ).firstOrNull { it != null && !it.isEmpty }?.item
        } ?: throw SimpleCommandExceptionType(Text.literal("No item to find")).create()


        var amount = 0
        findContainers(world, player.blockPos, range, item).forEach { position ->
            amount++
            Marker.spawn(world, position)
        }

        val message = Text.empty().apply {
            if (amount == 0) {
                append(Text.literal("No "))
                append(Text.translatable(item.translationKey))
                append(Text.literal(" found!"))
            } else {
                append(Text.literal("Found $amount "))
                append(Text.translatable(item.translationKey))
            }
            style = Style.EMPTY.withColor(Formatting.RED)
        }

        player.sendMessage(message, true)

        return amount
    }

    private fun findContainers(world: World, center: BlockPos, range: Int, item: Item) = sequence<BlockPos> {
        val searchFor = setOf(item)
        for (x in center.x - range..center.x + range) {
            for (y in center.y - range..center.y + range) {
                for (z in center.z - range..center.z + range) {
                    val pos = BlockPos(x, y, z)
                    val block = world.getBlockEntity(pos)
                    if (block is Inventory && block.containsAny(searchFor)) {
                        yield(pos)
                    }
                }
            }
        }
    }
}