package dev.rodrick.acer.effect

import dev.rodrick.acer.AcerMod
import dev.rodrick.acer.annotations.Init
import dev.rodrick.acer.config.AcerConfig
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.block.CropBlock
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.loot.context.LootWorldContext
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.util.ActionResult
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

object Scythe {
    private val CROPS = BlockTags.CROPS
    private val SCYTHES = TagKey.of(RegistryKeys.ITEM, Identifier.of(AcerMod.MOD_ID, "scythes"))

    fun getScytheLevel(item: ItemStack): Int {
        for (level in 4 downTo 1) {
            val levelTag = TagKey.of(RegistryKeys.ITEM, Identifier.of(AcerMod.MOD_ID, "scythes_level_$level"))
            if (item.isIn(levelTag)) return level
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
                    blocks.add(center.add(x, 0, z))
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
        world: ServerWorld, blockPos: BlockPos, player: PlayerEntity, tool: ItemStack
    ): Boolean {
        val blockEntity = world.getBlockEntity(blockPos)
        val blockState = world.getBlockState(blockPos)

        if (!blockState.isIn(CROPS) || (blockState.block as? CropBlock)?.isMature(blockState) != true) {
            return false
        }

        val lootContext = LootWorldContext.Builder(world).add(LootContextParameters.ORIGIN, blockPos.toCenterPos())
            .add(LootContextParameters.TOOL, tool).addOptional(LootContextParameters.THIS_ENTITY, player)
            .addOptional(LootContextParameters.BLOCK_ENTITY, blockEntity)

        blockState.getDroppedStacks(lootContext).forEach { stack ->
            if ((stack.item as? BlockItem)?.block?.defaultState?.isIn(CROPS) == true) {
                stack.count--
            }

            if (stack.count > 0) {
                val pos = blockPos.toCenterPos()
                val entity = ItemEntity(world, pos.x, pos.y, pos.z, stack).apply {
                    setToDefaultPickupDelay()
                }

                world.spawnEntity(entity)
            }
        }

        world.playSound(
            null,
            blockPos.x.toDouble(),
            blockPos.y.toDouble(),
            blockPos.z.toDouble(),
            blockState.soundGroup.breakSound,
            SoundCategory.BLOCKS,
            0.6f,
            1.0f,
        )

        world.setBlockState(blockPos, blockState.block.defaultState)

        return true
    }

    @Init
    fun init() {
        UseBlockCallback.EVENT.register { player, world, hand, hitResult ->
            val enabled = AcerConfig.data.scythes
            if (!enabled) return@register ActionResult.PASS

            val blockPos = hitResult.blockPos
            val heldStack = player.getStackInHand(hand)
            val serverWorld = world as? ServerWorld

            if (!heldStack.isIn(SCYTHES)) {
                return@register ActionResult.PASS
            }

            if (player.itemCooldownManager.isCoolingDown(heldStack)) {
                return@register ActionResult.FAIL
            }

            if (!world.getBlockState(blockPos).isIn(BlockTags.CROPS)) {
                return@register ActionResult.PASS
            }

            if (serverWorld != null) {
                var brokenBlocks = 0

                if (player.isSneaking) {
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
                    heldStack.damage(
                        brokenBlocks, player, hand.equipmentSlot
                    )

                    val efficiencyLevel = EnchantmentHelper.getLevel(
                        world.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY),
                        heldStack
                    )

                    player.itemCooldownManager.set(heldStack, 30 - efficiencyLevel * 6)
                    player.swingHand(hand, true)

                    ActionResult.SUCCESS
                } else {
                    ActionResult.PASS
                }
            }

            ActionResult.PASS
        }
    }
}