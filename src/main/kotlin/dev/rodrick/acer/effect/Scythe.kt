package dev.rodrick.acer.effect

import dev.rodrick.acer.AcerMod
import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams

object Scythe {
    private val CROPS = BlockTags.CROPS
    private val SCYTHES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AcerMod.MOD_ID, "scythes"))

    fun getScytheLevel(item: ItemStack): Int {
        for (level in 4 downTo 1) {
            val levelTag =
                TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(AcerMod.MOD_ID, "scythes_level_$level"))
            if (item.`is`(levelTag)) return level
        }

        return 0
    }

    fun getBlocks(center: BlockPos, level: Int): Set<BlockPos> {
        fun makePlus(center: BlockPos, distance: Int = 1): Set<BlockPos> {
            return setOf(
                center, center.north(distance), center.west(distance), center.east(distance), center.south(distance)
            )
        }

        fun makeSquare(center: BlockPos, distance: Int): Set<BlockPos> {
            val blocks = mutableSetOf<BlockPos>()
            for (x in -distance..distance) {
                for (z in -distance..distance) {
                    blocks.add(center.offset(x, 0, z))
                }
            }
            return blocks.toSet()
        }

        return when {
            level <= 0 -> setOf(center)
            level == 1 -> makePlus(center, 1)
            level == 2 -> makeSquare(center, 1)
            level == 3 -> makePlus(center, 2) + makeSquare(center, 1)
            else -> makeSquare(center, 2)
        }
    }

    fun handleBlock(
        world: ServerLevel, blockPos: BlockPos, player: Player, tool: ItemStack
    ): Boolean {
        val blockEntity = world.getBlockEntity(blockPos)
        val blockState = world.getBlockState(blockPos)

        if (!blockState.`is`(CROPS) || (blockState.block as? CropBlock)?.isMaxAge(blockState) != true) {
            return false
        }

        val lootContext = LootParams.Builder(world).withParameter(LootContextParams.ORIGIN, blockPos.center)
            .withParameter(LootContextParams.TOOL, tool).withOptionalParameter(LootContextParams.THIS_ENTITY, player)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)

        blockState.getDrops(lootContext).forEach { stack ->
            if ((stack.item as? BlockItem)?.block?.defaultBlockState()?.`is`(CROPS) == true) {
                stack.count--
            }

            if (stack.count > 0) {
                val pos = blockPos.center
                val entity = ItemEntity(world, pos.x, pos.y, pos.z, stack).apply {
                    setDefaultPickUpDelay()
                }

                world.addFreshEntity(entity)
            }
        }

        world.playSound(
            null,
            blockPos.x.toDouble(),
            blockPos.y.toDouble(),
            blockPos.z.toDouble(),
            blockState.soundType.breakSound,
            SoundSource.BLOCKS,
            0.6f,
            1.0f,
        )

        world.setBlockAndUpdate(blockPos, blockState.block.defaultBlockState())

        return true
    }

    @Init
    fun init() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            val enabled = AcerConfig.data.scythes
            if (!enabled) return@register InteractionResult.PASS

            val blockPos = hitResult.blockPos
            val heldStack = player.getItemInHand(hand)
            val serverWorld = world as? ServerLevel

            if (!heldStack.`is`(SCYTHES)) {
                return@register InteractionResult.PASS
            }

            if (player.cooldowns.isOnCooldown(heldStack)) {
                return@register InteractionResult.FAIL
            }

            if (!world.getBlockState(blockPos).`is`(BlockTags.CROPS)) {
                return@register InteractionResult.PASS
            }

            if (serverWorld != null) {
                var brokenBlocks = 0

                if (player.isShiftKeyDown) {
                    val level = getScytheLevel(heldStack)
                    for (block in getBlocks(blockPos, level)) {
                        if (handleBlock(serverWorld, block, player, heldStack)) {
                            brokenBlocks++
                        }
                    }
                } else {
                    if (handleBlock(serverWorld, blockPos, player, heldStack)) {
                        brokenBlocks++
                    }
                }

                if (brokenBlocks > 0) {
                    heldStack.hurtAndBreak(
                        brokenBlocks, player, hand.asEquipmentSlot()
                    )

                    val efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(
                        world.registryAccess().getOrThrow(Enchantments.EFFICIENCY), heldStack
                    )

                    player.cooldowns.addCooldown(heldStack, 30 - efficiencyLevel * 6)
                    player.swing(hand, true)

                    InteractionResult.SUCCESS
                } else {
                    InteractionResult.PASS
                }
            }

            InteractionResult.PASS
        }
    }
}